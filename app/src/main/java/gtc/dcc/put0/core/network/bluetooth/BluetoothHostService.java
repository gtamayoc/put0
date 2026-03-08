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
    private java.util.Timer reconnectionTimer;

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
     * Alternative constructor for host promotion.
     * Accepts an existing GameState so the game continues seamlessly
     * when a client takes over as the new host.
     */
    public BluetoothHostService(BluetoothAdapter adapter, HostListener listener,
            com.game.core.model.GameState existingState, String newHostPlayerId) {
        this.bluetoothAdapter = adapter;
        this.listenerRef = new WeakReference<>(listener);
        this.gameEngine = new GameEngine();
        this.gson = new Gson();
        this.gameState = existingState;
        this.gameState.setStatus(com.game.core.model.GameStatus.PAUSED); // Paused until old host reconnects

        // Mark players correctly: the new host is connected, the old host is
        // disconnected
        for (com.game.core.model.Player p : this.gameState.getPlayers()) {
            if (p.getId().equals(newHostPlayerId)) {
                p.setConnected(true);
            } else {
                p.setConnected(false);
                p.setDisconnectedTime(System.currentTimeMillis());
            }
        }

        startReconnectionTimer();
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
        host.setConnected(true); // Host is always connected
        gameState.addPlayer(host); // Add host ONCE

        startAcceptThread();

        broadcastState();
    }

    /**
     * Starts or restarts the 60-second timer for player reconnection.
     * If the timer elapses, the connected player wins by abandonment.
     */
    private synchronized void startReconnectionTimer() {
        if (reconnectionTimer != null) {
            reconnectionTimer.cancel();
        }
        reconnectionTimer = new java.util.Timer();
        reconnectionTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (gameState != null && gameState.getStatus() == com.game.core.model.GameStatus.PAUSED) {
                    gameState.setStatus(com.game.core.model.GameStatus.FINISHED);
                    gameState.setWonByAbandonment(true); // Mark as abandoned

                    // The connected player wins by default
                    String winnerId = gameState.getPlayers().get(0).getId();
                    for (com.game.core.model.Player p : gameState.getPlayers()) {
                        if (p.isConnected()) {
                            winnerId = p.getId();
                            break;
                        }
                    }
                    gameState.setWinnerId(winnerId);

                    broadcastState();
                    HostListener l1 = listenerRef.get();
                    if (l1 != null) {
                        l1.onError("Tiempo de reconexión agotado. Partida terminada por abandono.");
                    }
                }
            }
        }, 60000);
    }

    private synchronized void startAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
        CoreLogger.d("BT-HOST: AcceptThread started, waiting for connections with UUID: " + APP_UUID);
    }

    /**
     * Public entry point to start accepting incoming connections.
     * Used when this device is promoted to host mid-game.
     */
    public void startAcceptingConnections() {
        startAcceptThread();
        broadcastState(); // Send current state to any connected client
    }

    /**
     * Starts the game once players are ready.
     */
    public void startGame() {
        if (gameState == null) {
            HostListener l = listenerRef.get();
            if (l != null)
                l.onError("Cannot start game: no game state.");
            return;
        }

        if (gameState.getPlayers() == null || gameState.getPlayers().size() < 2) {
            HostListener l = listenerRef.get();
            if (l != null)
                l.onError("No hay suficientes jugadores para iniciar (mínimo 2).");
            return;
        }

        if (gameState.getStatus() != com.game.core.model.GameStatus.WAITING) {
            HostListener l = listenerRef.get();
            if (l != null)
                l.onError("La partida ya fue iniciada o está en pausa.");
            return;
        }

        if (bluetoothConnection == null) {
            // Log warning but don't block — state will still be broadcast locally
            CoreLogger.w("BT-HOST: startGame called but bluetoothConnection is null. Client may not receive update.");
        }

        gameState.setGameStartTime(System.currentTimeMillis()); // Stamp start time
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
                case RESTART_GAME:
                    // Reset abandonment flag and re-stamp start time for the new game
                    gameState.setWonByAbandonment(false);
                    gameState.setGameStartTime(System.currentTimeMillis());
                    gameEngine.restartGame(gameState);
                    break;
                case SET_PAUSED:
                    gameState.setStatus(com.game.core.model.GameStatus.PAUSED);
                    break;
            }
        } catch (Exception e) {
            CoreLogger.e("BT-HOST: Error applying event: " + e.getMessage());
            if (event.getPlayerId().startsWith("client_")) {
                if (bluetoothConnection != null) {
                    bluetoothConnection.write("ERROR:" + e.getMessage());
                }
            } else {
                HostListener l = listenerRef.get();
                if (l != null) {
                    l.onError(e.getMessage());
                }
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
                // Android 15 compatibility: Try SECURE server socket first.
                // It requires bonding but provides a more stable, encrypted channel.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
                CoreLogger.d("BT-HOST: Created SECURE server socket.");
            } catch (IOException e) {
                CoreLogger.w("BT-HOST: Secure server socket failed, trying insecure: " + e.getMessage());
                try {
                    // Fallback to INSECURE socket for non-bonded pairings or older devices.
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
                    CoreLogger.d("BT-HOST: Created INSECURE server socket (fallback).");
                } catch (IOException e2) {
                    CoreLogger.e("BT-HOST: Both server socket variants failed: " + e2.getMessage());
                } catch (SecurityException e2) {
                    CoreLogger.e("BT-HOST: Missing permissions for insecure server socket fallback.");
                }
            } catch (SecurityException e) {
                CoreLogger.e("BT-HOST: Missing Bluetooth permissions for secure server socket creation.");
                // Last-ditch effort if permissions are somehow partially granted
                try {
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
                } catch (IOException | SecurityException ignored) {
                }
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
        final String clientAddress;
        final String clientId;
        String devNameTemp = "Unknown Device";
        try {
            clientAddress = socket.getRemoteDevice().getAddress();
            clientId = "client_" + clientAddress;
            devNameTemp = socket.getRemoteDevice().getName();
            if (devNameTemp == null)
                devNameTemp = "Unknown Device";
        } catch (SecurityException e) {
            CoreLogger.e("BT-HOST: SecurityException getting remote device info");
            return;
        }
        final String devName = devNameTemp;

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
                CoreLogger.w("BT-HOST: Connection lost. Pausing game and starting AcceptThread for reconnections.");
                if (gameState != null) {
                    gameState.setStatus(com.game.core.model.GameStatus.PAUSED);
                    for (Player p : gameState.getPlayers()) {
                        if (p.getId().equals(clientId)) {
                            p.setConnected(false);
                            p.setDisconnectedTime(System.currentTimeMillis());
                            break;
                        }
                    }
                    broadcastState();
                    startAcceptThread(); // Allow reconnecting
                    startReconnectionTimer(); // Start 60s timeout
                }
                HostListener l = listenerRef.get();
                if (l != null)
                    l.onError("Se ha perdido la conexión. Partida pausada (60s para reconectar).");
            }
        });
        bluetoothConnection.start();

        HostListener l = listenerRef.get();
        if (l != null) {
            // Add or Reconnect client in state
            if (gameState != null) {
                boolean found = false;
                for (Player p : gameState.getPlayers()) {
                    if (p.getId().equals(clientId)) {
                        p.setConnected(true);
                        devNameTemp = p.getName(); // keep old name
                        found = true;
                        break;
                    }
                }
                // If not found by exact client MAC ID, see if there's a disconnected player we
                // can reclaim (e.g. old host rejoining)
                if (!found) {
                    for (Player p : gameState.getPlayers()) {
                        if (!p.isConnected()) {
                            p.setConnected(true);
                            devNameTemp = p.getName();
                            found = true;
                            CoreLogger.i("BT-HOST: Reclaimed disconnected player slot for: " + p.getId());
                            break;
                        }
                    }
                }

                if (!found) {
                    Player client = new Player(clientId, devName, false);
                    client.setConnected(true); // New client is connected
                    gameState.addPlayer(client);
                }

                // Cancel reconnection timer if active
                if (reconnectionTimer != null) {
                    reconnectionTimer.cancel();
                    reconnectionTimer = null;
                    CoreLogger.i("BT-HOST: Reconnection timer cancelled. Client reconnected.");
                }

                // Automatically resume if not waiting
                if (gameState.getStatus() == com.game.core.model.GameStatus.PAUSED) {
                    boolean allConnected = true;
                    for (Player p : gameState.getPlayers()) {
                        if (!p.isConnected()) {
                            allConnected = false;
                            break;
                        }
                    }
                    if (allConnected) {
                        gameState.setStatus(com.game.core.model.GameStatus.PLAYING);
                        // Clear error or pause message if needed
                        HostListener l1 = listenerRef.get();
                        if (l1 != null) {
                            l1.onError("Jugador reconectado. Reanudando partida...");
                        }
                    }
                }
                broadcastState();
            }
            l.onClientConnected(devName);
        } else {
            CoreLogger.w("BT-HOST: Listener GC'd before client connected — state will still broadcast.");
            // Still broadcast state even if listener is gone (client needs the update)
            if (gameState != null) {
                broadcastState();
            }
        }
    }
}
