package gtc.dcc.put0.core.network.bluetooth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.game.core.model.Player;
import com.google.gson.Gson; // Added for CoreLogger.json

import java.util.List;

import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.local.GameMapper;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.network.MatchManager;
import gtc.dcc.put0.core.utils.CoreLogger; // Added for logging
import com.game.core.network.GameAction;
import com.game.core.network.GameEventDTO;

/**
 * MatchManager implementation for Bluetooth Offline Multiplayer.
 * Adapts Bluetooth events and models to the Android app's architecture.
 */
public class BluetoothMatchManager implements MatchManager,
        BluetoothHostService.HostListener,
        BluetoothClientService.ClientListener {
    private static BluetoothMatchManager instance;

    private final MutableLiveData<GameState> _gameState = new MutableLiveData<>();
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    private final MutableLiveData<String> _currentGameId = new MutableLiveData<>();
    private final MutableLiveData<String> _currentPlayerId = new MutableLiveData<>();

    private BluetoothHostService hostService;
    private BluetoothClientService clientService;
    private boolean isHost = false;

    private BluetoothMatchManager() {
    }

    public static synchronized BluetoothMatchManager getInstance() {
        if (instance == null) {
            instance = new BluetoothMatchManager();
        }
        return instance;
    }

    public void initAsHost(BluetoothHostService hostService, String hostPlayerId, String hostName) {
        reset();
        this.hostService = hostService;
        this.isHost = true;
        this.clientService = null;
        this._currentPlayerId.setValue(hostPlayerId);
    }

    public void initAsClient(BluetoothClientService clientService, String clientPlayerId, String clientName) {
        reset();
        this.clientService = clientService;
        this.isHost = false;
        this.hostService = null;
        this._currentPlayerId.setValue(clientPlayerId);
    }

    private void reset() {
        _gameState.setValue(null);
        _currentGameId.setValue(null);
        _error.setValue(null);
    }

    // Called by Bluetooth callbacks (may be on BT thread or main thread)
    // postValue() is always safe — it dispatches to the main thread internally.
    public void onStateUpdated(com.game.core.model.GameState coreState) {
        if (coreState == null)
            return;
        try {
            gtc.dcc.put0.core.data.model.GameState androidState = GameMapper.toAndroidState(coreState);
            CoreLogger.i("BT-MANAGER: onStateUpdated — status=" + androidState.getStatus()
                    + ", players=" + (androidState.getPlayers() != null ? androidState.getPlayers().size() : 0));

            // CRITICAL FIX: The Host assigns a specific ID ("client_<MAC>") to the Client.
            // If we are the Client, we MUST adopt this ID so that GameActivity can
            // recognise
            // our own cards. In a 2-player BT game, Host is always index 0, Client is index
            // 1.
            if (!isHost && androidState.getPlayers() != null && androidState.getPlayers().size() >= 2) {
                String assignedClientId = androidState.getPlayers().get(1).getId();
                if (assignedClientId != null && !assignedClientId.equals(_currentPlayerId.getValue())) {
                    CoreLogger.i("BT-MANAGER: Adopting host-assigned Client ID: " + assignedClientId);
                    _currentPlayerId.postValue(assignedClientId);
                }
            }

            _gameState.postValue(androidState);
            if (androidState.getGameId() != null) {
                _currentGameId.postValue(androidState.getGameId());
            }
        } catch (Exception e) {
            CoreLogger.e("BT-MANAGER: Error mapping GameState: " + e.getMessage());
        }
    }

    public void onError(String message) {
        _error.postValue(message);
    }

    // --- Listener Implementations ---

    @Override
    public void onClientConnected(String deviceName) {
        // Log connection or handle specific Host UI updates if needed
        CoreLogger.i("BT-MANAGER: Client connected: " + deviceName);
    }

    @Override
    public void onConnected(String deviceName) {
        // Log connection or handle specific Client UI updates if needed
        CoreLogger.i("BT-MANAGER: Connected to host: " + deviceName);
    }

    // MatchManager interface

    public void startGame() {
        if (isHost && hostService != null) {
            CoreLogger.i("BT-MANAGER: Starting game via HostService");
            hostService.startGame();
        }
    }

    @Override
    public LiveData<GameState> getGameState() {
        return _gameState;
    }

    @Override
    public LiveData<String> getError() {
        return _error;
    }

    @Override
    public LiveData<String> getCurrentGameId() {
        return _currentGameId;
    }

    @Override
    public LiveData<String> getCurrentPlayerId() {
        return _currentPlayerId;
    }

    @Override
    public void playCard(String playerId, Card card) {
        List<String> payload = java.util.Collections.singletonList(card.getInstanceId());
        GameEventDTO event = new GameEventDTO(com.game.core.network.GameAction.PLAY_CARD, playerId, payload);
        sendEvent(event);
    }

    @Override
    public void playCards(String playerId, List<Card> cards) {
        List<String> payload = new java.util.ArrayList<>();
        for (Card c : cards) {
            payload.add(c.getInstanceId());
        }
        GameEventDTO event = new GameEventDTO(com.game.core.network.GameAction.PLAY_CARD, playerId, payload);
        sendEvent(event);
    }

    @Override
    public void drawCard(String playerId) {
        GameEventDTO event = new GameEventDTO(com.game.core.network.GameAction.DRAW_CARD, playerId, null);
        sendEvent(event);
    }

    @Override
    public void collectTable(String playerId) {
        GameEventDTO event = new GameEventDTO(com.game.core.network.GameAction.COLLECT_TABLE, playerId, null);
        sendEvent(event);
    }

    @Override
    public void leaveGame() {
        if (hostService != null) {
            hostService.stop();
            hostService = null;
        }
        if (clientService != null) {
            clientService.stop();
            clientService = null;
        }
        // FIX: Clear ALL LiveData so stale data doesn't bleed into the next session.
        _gameState.postValue(null);
        _currentGameId.postValue(null);
        _currentPlayerId.postValue(null);
        _error.postValue(null);
    }

    /**
     * Destroys the Singleton instance and releases all service references.
     * Call this when the user fully exits the Bluetooth multiplayer flow
     * (e.g. in onDestroy of the host/client Activity) so the GC can collect
     * the Activity and its associated services.
     *
     * After calling destroy(), the next call to getInstance() will create a fresh
     * instance.
     */
    public static synchronized void destroy() {
        if (instance != null) {
            instance.leaveGame();
            instance = null;
            CoreLogger.d("BT-MANAGER: Singleton destroyed and all resources released.");
        }
    }

    private void sendEvent(GameEventDTO event) {
        if (isHost && hostService != null) {
            hostService.processHostAction(event);
        } else if (!isHost && clientService != null) {
            clientService.sendAction(event);
        }
    }
}
