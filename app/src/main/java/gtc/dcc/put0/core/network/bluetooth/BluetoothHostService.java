package gtc.dcc.put0.core.network.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import gtc.dcc.put0.core.utils.CoreLogger;

import com.game.core.engine.GameEngine;
import com.game.core.model.Card;
import com.game.core.model.GameState;
import com.game.core.model.Player;
import com.game.core.network.GameAction;
import com.game.core.network.GameEventDTO;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BluetoothHostService {
    private static final String TAG = "BluetoothHostService";
    // Fixed UUID for the PUT0 game. Must be same on Client and Host.
    public static final UUID APP_UUID = UUID.fromString("1f3f7ea2-2e55-4a55-8d5f-fd4f42ec5301");
    private static final String APP_NAME = "PUT0 Game";

    private final BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private BluetoothConnection bluetoothConnection;

    private final GameEngine gameEngine;
    private GameState gameState;
    private final Gson gson;

    /**
     * WeakReference to the listener (typically a LobbyActivity anonymous class).
     * Using WeakReference prevents the service from retaining the Activity in
     * memory
     * after it is destroyed (e.g., when navigating to GameActivity).
     * The BluetoothConnection thread would otherwise keep this service alive,
     * which in turn would keep the Activity alive — causing a ~300 kB leak.
     */
    private WeakReference<HostListener> listenerRef;

    public interface HostListener {
        void onClientConnected(String deviceName);

        void onStateUpdated(GameState newState);

        void onError(String message);
    }

    public BluetoothHostService(BluetoothAdapter adapter, HostListener listener) {
        this.bluetoothAdapter = adapter;
        this.listenerRef = new WeakReference<>(listener);
        this.gameEngine = new GameEngine();
        this.gson = new Gson();
    }

    /**
     * Explicitly clears the listener reference.
     * Call from Activity.onDestroy() to eagerly break the leak chain
     * without waiting for GC to clear the WeakReference.
     */
    public void clearListener() {
        listenerRef = new WeakReference<>(null);
        CoreLogger.d("BT-HOST: Listener cleared (Activity detached).");
    }

    public synchronized void start(String hostPlayerId, String hostName, int deckSize) {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Initialize state for Lobby
        gameState = new GameState(UUID.randomUUID().toString());
        gameState.setDeckSize(deckSize);
        gameState.setStatus(com.game.core.model.GameStatus.WAITING);
        Player host = new Player(hostPlayerId, hostName, false);
        gameState.addPlayer(host);

        acceptThread = new AcceptThread();
        acceptThread.start();
        CoreLogger.d("BT-HOST: Service started, waiting for connections with UUID: " + APP_UUID);

        broadcastState();
    }

    /**
     * Starts the game once players are ready.
     */
    public void startGame() {
        if (gameState == null || bluetoothConnection == null) {
            HostListener l = listenerRef.get();
            if (l != null)
                l.onError("Cannot start game: no client connected.");
            return;
        }

        gameEngine.startGame(gameState);
        broadcastState();
    }

    /**
     * Called by the local Host player's UI to apply an action.
     */
    public void processHostAction(GameEventDTO event) {
        if (gameState == null)
            return;
        applyEventToEngine(event);
        broadcastState();
    }

    private void broadcastState() {
        if (gameState == null)
            return;

        // Notify client if connected
        if (bluetoothConnection != null) {
            String json = gson.toJson(gameState);
            bluetoothConnection.write(json);
        }

        // Notify local UI only if the listener (Activity) is still alive
        HostListener l = listenerRef.get();
        if (l != null) {
            l.onStateUpdated(gameState);
        }
    }

    private void applyEventToEngine(GameEventDTO event) {
        try {
            switch (event.getAction()) {
                case PLAY_CARD:
                    if (event.getPayload() != null && !event.getPayload().isEmpty()) {
                        // GameEngine currently only supports 1 card played in playCard.
                        // For multiple "playCards", we would pass the whole list.
                        if (event.getPayload().size() == 1) {
                            String cardId = event.getPayload().get(0);
                            Card cardToPlay = findCardInPlayerHand(event.getPlayerId(), cardId);
                            if (cardToPlay != null) {
                                gameEngine.playCard(gameState, event.getPlayerId(), cardToPlay);
                            } else {
                                CoreLogger.w("BT-HOST: Card not found in any pile: " + cardId);
                            }
                        } else {
                            // Multiple cards
                            java.util.List<Card> cards = new java.util.ArrayList<>();
                            for (String cid : event.getPayload()) {
                                Card c = findCardInPlayerHand(event.getPlayerId(), cid);
                                if (c != null)
                                    cards.add(c);
                            }
                            if (!cards.isEmpty()) {
                                gameEngine.playCards(gameState, event.getPlayerId(), cards);
                            }
                        }
                    }
                    break;
                case DRAW_CARD:
                    gameEngine.drawCard(gameState, event.getPlayerId());
                    break;
                case COLLECT_TABLE:
                    gameEngine.collectTable(gameState, event.getPlayerId());
                    break;
                case PASS:
                    // Only used if passing turn without collecting
                    break;
            }
        } catch (Exception e) {
            CoreLogger.e("BT-HOST: Error applying event: " + e.getMessage());
            HostListener l = listenerRef.get();
            if (l != null) {
                l.onError(e.getMessage());
            }
        }
    }

    private Card findCardInPlayerHand(String playerId, String cardInstanceId) {
        for (Player p : gameState.getPlayers()) {
            if (p.getId().equals(playerId)) {
                for (Card c : p.getHand()) {
                    if (c.getInstanceId().equals(cardInstanceId))
                        return c;
                }
                for (Card c : p.getVisibleCards()) {
                    if (c.getInstanceId().equals(cardInstanceId))
                        return c;
                }
                for (Card c : p.getHiddenCards()) {
                    if (c.getInstanceId().equals(cardInstanceId))
                        return c;
                }
            }
        }
        return null;
    }

    public synchronized void stop() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if (bluetoothConnection != null) {
            bluetoothConnection.cancel();
            bluetoothConnection = null;
        }
        CoreLogger.d("BT-HOST: Service stopped.");
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // Requires BLUETOOTH_CONNECT permission on Android 12+ (which we checked before
                // starting)
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                CoreLogger.e("BT-HOST: Socket's listen() method failed: " + e.getMessage());
            } catch (SecurityException e) {
                CoreLogger.e("BT-HOST: Missing Bluetooth permissions for listen()");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (mmServerSocket == null) {
                CoreLogger.e("BT-HOST: AcceptThread started with null ServerSocket, aborting.");
                HostListener l = listenerRef.get();
                if (l != null)
                    l.onError("Bluetooth server socket could not be created.");
                return;
            }
            BluetoothSocket socket = null;
            while (true) {
                try {
                    CoreLogger.d("BT-HOST: AcceptThread listening for incoming connections...");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // This is expected when cancel() closes the ServerSocket intentionally.
                    CoreLogger.e("BT-HOST: Socket's accept() method failed: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    CoreLogger.e("BT-HOST: Unexpected error in accept(): " + e.getMessage());
                    break;
                }

                if (socket != null) {
                    CoreLogger.d("BT-HOST: Client socket accepted from: " + socket.getRemoteDevice().getAddress());
                    manageConnectedSocket(socket);
                    try {
                        // Close the ServerSocket after accepting one client (1-client design).
                        mmServerSocket.close();
                    } catch (IOException e) {
                        CoreLogger.e("BT-HOST: Could not close ServerSocket: " + e.getMessage());
                    }
                    break; // Only 1 client supported
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null)
                    mmServerSocket.close();
            } catch (IOException e) {
                CoreLogger.e("BT-HOST: Could not close the server socket: " + e.getMessage());
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        // FIX: Cancel any pre-existing connection to avoid leaking the previous thread.
        if (bluetoothConnection != null) {
            CoreLogger.w("BT-HOST: New connection accepted while previous was active. Cancelling old one.");
            bluetoothConnection.cancel();
            bluetoothConnection = null;
        }
        bluetoothConnection = new BluetoothConnection(socket, new BluetoothConnection.ConnectionListener() {
            @Override
            public void onMessageReceived(String message) {
                try {
                    GameEventDTO event = gson.fromJson(message, GameEventDTO.class);
                    if (event != null && event.getAction() != null) {
                        applyEventToEngine(event);
                        broadcastState();
                    }
                } catch (Exception e) {
                    CoreLogger.e("BT-HOST: Failed to parse incoming GameEvent: " + e.getMessage());
                }
            }

            @Override
            public void onConnectionLost() {
                HostListener l = listenerRef.get();
                if (l != null)
                    l.onError("Connection lost with client.");
            }
        });
        bluetoothConnection.start();

        HostListener l = listenerRef.get();
        if (l != null) {
            try {
                String devName = socket.getRemoteDevice().getName();
                if (devName == null)
                    devName = "Unknown Device";

                // Add client to the lobby state
                if (gameState != null) {
                    Player client = new Player(
                            "client_" + socket.getRemoteDevice().getAddress(), devName, false);
                    gameState.addPlayer(client);
                    broadcastState();
                }

                l.onClientConnected(devName);
            } catch (SecurityException e) {
                l.onClientConnected("Client");
            }
        } else {
            CoreLogger.w("BT-HOST: Listener GC'd before client connected — state will still broadcast.");
            // Still broadcast state even if listener is gone (client needs the update)
            if (gameState != null) {
                broadcastState();
            }
        }
    }
}
