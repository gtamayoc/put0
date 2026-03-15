package gtc.dcc.put0.core.network.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import gtc.dcc.put0.core.utils.CoreLogger;

import com.game.core.model.GameState;
import com.game.core.network.GameEventDTO;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BluetoothClientService {
    private static final String TAG = "BluetoothClientService";

    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private BluetoothConnection bluetoothConnection;
    /** Last game state received from the host; used for host promotion. */
    private volatile GameState lastReceivedState;
    private ScheduledExecutorService hostTimeoutExecutor;

    private final Gson gson;
    /**
     * WeakReference to avoid retaining LobbyActivity after it is destroyed.
     * Same leak pattern as BluetoothHostService. See BluetoothHostService for full
     * explanation.
     */
    private WeakReference<ClientListener> listenerRef;

    public interface ClientListener {
        void onConnected(String deviceName);

        void onStateUpdated(GameState newState);

        void onError(String message);

        void onClientIdAssigned(String id);

        /**
         * Called when the host disconnects and doesn't reconnect within the timeout.
         * The UI/Manager should switch this device to host mode.
         */
        void onShouldBecomeHost(GameState lastState);
    }

    public BluetoothClientService(BluetoothAdapter adapter, ClientListener listener) {
        this.bluetoothAdapter = adapter;
        this.listenerRef = new WeakReference<>(listener);
        this.gson = new Gson();
    }

    /**
     * Returns the BluetoothAdapter; used by BluetoothMatchManager for host
     * promotion.
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /** Eagerly clears the listener. Call from Activity.onDestroy(). */
    public void clearListener() {
        listenerRef = new WeakReference<>(null);
        CoreLogger.d("BT-CLIENT: Listener cleared (Activity detached).");
    }

    public synchronized void connect(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        CoreLogger.d("BT-CLIENT: Attempting to connect to host: " + device.getAddress());
    }

    private volatile boolean isRunning = true;

    /**
     * Send an action to the host.
     */
    public void sendAction(GameEventDTO event) {
        if (bluetoothConnection != null) {
            String json = gson.toJson(event);
            bluetoothConnection.write(json);
        }
    }

    public synchronized void stop() {
        isRunning = false;
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (bluetoothConnection != null) {
            bluetoothConnection.cancel();
            bluetoothConnection = null;
        }
        if (hostTimeoutExecutor != null) {
            hostTimeoutExecutor.shutdownNow();
            hostTimeoutExecutor = null;
        }
        CoreLogger.d("BT-CLIENT: Service stopped.");
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        public void run() {
            // Cancel discovery because it slows down the connection.
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                CoreLogger.e("BT-CLIENT: Missing permissions for cancelDiscovery");
            }

            boolean connected = false;

            // Step 1: Try SECURE
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(BluetoothHostService.APP_UUID);
                CoreLogger.d("BT-CLIENT: Connecting via SECURE socket...");
                mmSocket.connect();
                connected = true;
                CoreLogger.d("BT-CLIENT: SECURE connect() succeeded!");
            } catch (IOException connectException) {
                CoreLogger.w("BT-CLIENT: SECURE connect() failed: " + connectException.getMessage());
                try {
                    mmSocket.close();
                } catch (Exception closeException) {
                }
            } catch (SecurityException e) {
                CoreLogger.e("BT-CLIENT: Missing perms for SECURE connection");
            }

            // Step 2: Try INSECURE if SECURE failed
            if (!connected) {
                try {
                    mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(BluetoothHostService.APP_UUID);
                    CoreLogger.d("BT-CLIENT: Connecting via INSECURE socket...");
                    mmSocket.connect();
                    connected = true;
                    CoreLogger.d("BT-CLIENT: INSECURE connect() succeeded!");
                } catch (IOException connectException) {
                    CoreLogger.e("BT-CLIENT: INSECURE connect() also failed: " + connectException.getMessage());
                    try {
                        mmSocket.close();
                    } catch (Exception closeException) {
                    }
                } catch (SecurityException e) {
                    CoreLogger.e("BT-CLIENT: Missing perms for INSECURE connection");
                }
            }

            if (!connected) {
                CoreLogger.e("BT-CLIENT: Unable to connect via either SECURE or INSECURE methods.");
                ClientListener l = listenerRef.get();
                if (l != null)
                    l.onError("No se pudo conectar al anfitrión. Por favor, intente nuevamente.");
                return;
            }

            manageConnectedSocket(mmSocket);
        }

        public void cancel() {
            try {
                if (mmSocket != null)
                    mmSocket.close();
            } catch (IOException e) {
                CoreLogger.e("BT-CLIENT: Could not close the client socket: " + e.getMessage());
            }
        }

    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        bluetoothConnection = new BluetoothConnection(socket, new BluetoothConnection.ConnectionListener() {
            @Override
            public void onMessageReceived(String message) {
                try {
                    if (message.startsWith("ERROR:")) {
                        ClientListener l = listenerRef.get();
                        if (l != null) {
                            l.onError(message.substring(6));
                        }
                        return;
                    }
                    if (message.startsWith("ASSIGNED_ID:")) {
                        String assignedId = message.substring(12);
                        ClientListener l = listenerRef.get();
                        if (l != null) {
                            CoreLogger.d("BT-CLIENT: Received assigned ID: " + assignedId);
                            l.onClientIdAssigned(assignedId);
                        }
                        return;
                    }
                    GameState state = gson.fromJson(message, GameState.class);
                    if (state != null) {
                        lastReceivedState = state; // Cache for host promotion
                        ClientListener l = listenerRef.get();
                        CoreLogger.d("BT-CLIENT: GameState received. Listener alive: " + (l != null));
                        if (l != null)
                            l.onStateUpdated(state);
                    }
                } catch (Exception e) {
                    CoreLogger.e("BT-CLIENT: Failed to parse incoming GameState: " + e.getMessage());
                }
            }

            @Override
            public void onConnectionLost() {
                if (!isRunning) {
                    CoreLogger.d("BT-CLIENT: Connection lost due to manual stop, ignoring.");
                    return;
                }

                CoreLogger.w("BT-CLIENT: Connection to host lost. Promoting to host immediately.");
                ClientListener l = listenerRef.get();
                if (l != null)
                    l.onError("Conexión con el anfitrión perdida. Transfiriendo rol de anfitrión y esperando...");

                if (l != null && lastReceivedState != null) {
                    // Start promotion immediately so the previous host can reconnect as client
                    // without delay
                    l.onShouldBecomeHost(lastReceivedState);
                }
            }
        });
        bluetoothConnection.start();

        try {
            String devName = socket.getRemoteDevice().getName();
            if (devName == null)
                devName = "Unknown Host";
            ClientListener l = listenerRef.get();
            if (l != null)
                l.onConnected(devName);
        } catch (SecurityException e) {
            ClientListener l = listenerRef.get();
            if (l != null)
                l.onConnected("Host");
        }
    }
}
