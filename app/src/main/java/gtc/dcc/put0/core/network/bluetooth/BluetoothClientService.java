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

public class BluetoothClientService {
    private static final String TAG = "BluetoothClientService";

    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private BluetoothConnection bluetoothConnection;
    /** Last game state received from the host; used for host promotion. */
    private volatile GameState lastReceivedState;
    private java.util.Timer hostTimeoutTimer;

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
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (bluetoothConnection != null) {
            bluetoothConnection.cancel();
            bluetoothConnection = null;
        }
        CoreLogger.d("BT-CLIENT: Service stopped.");
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;

            // Android 15 (API 35) tightened the Bluetooth stack: insecure RFCOMM sockets
            // may be rejected when the devices are bonded and the system enforces
            // authenticated channels. Strategy:
            // 1. Try the SECURE socket (createRfcommSocketToServiceRecord) first — it uses
            // an encrypted, authenticated channel and works best on Android 12–15.
            // 2. Fall back to the INSECURE variant for older Android versions or edge
            // cases.
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(BluetoothHostService.APP_UUID);
                CoreLogger.d("BT-CLIENT: Created SECURE RFCOMM socket (preferred).");
            } catch (IOException e) {
                CoreLogger.w("BT-CLIENT: Secure socket creation failed, trying insecure: " + e.getMessage());
                try {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(BluetoothHostService.APP_UUID);
                    CoreLogger.d("BT-CLIENT: Created INSECURE RFCOMM socket (fallback).");
                } catch (IOException e2) {
                    CoreLogger.e("BT-CLIENT: Both socket variants failed: " + e2.getMessage());
                } catch (SecurityException e2) {
                    CoreLogger.e("BT-CLIENT: Missing permissions for insecure socket fallback.");
                }
            } catch (SecurityException e) {
                CoreLogger.e("BT-CLIENT: Missing Bluetooth permissions for secure socket creation.");
                // Last-ditch fallback
                try {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(BluetoothHostService.APP_UUID);
                } catch (IOException | SecurityException ignored) {
                }
            }
            mmSocket = tmp;
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

            try {
                mmSocket.connect();
                CoreLogger.d("BT-CLIENT: mmSocket.connect() succeeded!");
            } catch (IOException connectException) {
                CoreLogger.e("BT-CLIENT: Unable to connect: " + connectException.getMessage());
                ClientListener l = listenerRef.get();
                if (l != null)
                    l.onError("Could not connect to host.");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    CoreLogger.e("BT-CLIENT: Could not close socket: " + closeException.getMessage());
                }
                return;
            } catch (SecurityException e) {
                CoreLogger.e("BT-CLIENT: Missing permissions for connect");
                try {
                    mmSocket.close();
                } catch (IOException closeEx) {
                    CoreLogger.e("BT-CLIENT: Could not close socket after SecurityException: " + closeEx.getMessage());
                }
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
