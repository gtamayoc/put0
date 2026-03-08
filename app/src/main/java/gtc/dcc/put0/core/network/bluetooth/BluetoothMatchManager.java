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
    /** Emits true once when this device has been promoted to host during a game. */
    private final MutableLiveData<Boolean> _hostPromoted = new MutableLiveData<>();

    private BluetoothHostService hostService;
    private BluetoothClientService clientService;
    private boolean isHost = false;
    /**
     * Stored for host promotion: the device needs the adapter to start a
     * HostService.
     */
    private android.bluetooth.BluetoothAdapter bluetoothAdapter;

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
        this._currentPlayerId.postValue(hostPlayerId);
    }

    public void initAsClient(android.bluetooth.BluetoothAdapter adapter,
            BluetoothClientService clientService,
            String clientPlayerId, String clientName) {
        reset();
        this.bluetoothAdapter = adapter;
        this.clientService = clientService;
        this.isHost = false;
        this.hostService = null;
        this._currentPlayerId.postValue(clientPlayerId);
    }

    /** Returns true if this device is acting as the game host. */
    public boolean isHost() {
        return isHost;
    }

    /**
     * LiveData that emits true once when this device is promoted to host mid-game.
     */
    public LiveData<Boolean> getHostPromoted() {
        return _hostPromoted;
    }

    private void reset() {
        if (this.hostService != null) {
            this.hostService.stop();
            this.hostService = null;
        }
        if (this.clientService != null) {
            this.clientService.stop();
            this.clientService = null;
        }

        // Use postValue to safely mutate LiveData from any thread
        _gameState.postValue(null);
        _currentGameId.postValue(null);
        _error.postValue(null);

        // Clear any pending host-promotion flag so it is never replayed
        // to observers of a new session.
        _hostPromoted.postValue(null);
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
    public void onClientIdAssigned(String id) {
        CoreLogger.i("BT-MANAGER: Host explicitly assigned our Client ID: " + id);
        _currentPlayerId.postValue(id);
    }

    @Override
    public void onClientConnected(String deviceName) {
        // Log connection or handle specific Host UI updates if needed
        CoreLogger.i("BT-MANAGER: Client connected: " + deviceName);
    }

    @Override
    public void onConnected(String deviceName) {
        CoreLogger.i("BT-MANAGER: Connected to host: " + deviceName);
    }

    @Override
    public void onShouldBecomeHost(com.game.core.model.GameState lastState) {
        CoreLogger.w("BT-MANAGER: Promoting this device to host after host timeout.");
        // Stop client service
        if (clientService != null) {
            clientService.stop();
            clientService = null;
        }
        if (bluetoothAdapter == null || lastState == null) {
            CoreLogger.e("BT-MANAGER: Cannot promote — missing adapter or state.");
            return;
        }
        // Create a new HostService that continues the game from where it left off
        hostService = new BluetoothHostService(bluetoothAdapter, this, lastState, _currentPlayerId.getValue());
        isHost = true;
        hostService.startAcceptingConnections(); // Start listening for the old host to reconnect
        // Post true so the current observer sees the promotion event …
        _hostPromoted.postValue(true);
        // … then immediately clear it so new observers (next session) don't replay it.
        _hostPromoted.postValue(null);
        CoreLogger.i("BT-MANAGER: Promoted to host. Waiting for old host to reconnect.");
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

    @Override
    public void restartGame() {
        GameEventDTO event = new GameEventDTO(com.game.core.network.GameAction.RESTART_GAME,
                _currentPlayerId.getValue(), null);
        sendEvent(event);
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
