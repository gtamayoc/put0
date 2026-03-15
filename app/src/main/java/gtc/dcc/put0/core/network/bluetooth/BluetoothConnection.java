package gtc.dcc.put0.core.network.bluetooth;

import android.bluetooth.BluetoothSocket;
import gtc.dcc.put0.core.utils.CoreLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles raw data transmission over an established BluetoothSocket.
 * Uses DataInputStream/DataOutputStream with strict length-prefix framing.
 * This guarantees no packet fragmentation by ensuring the exact byte length
 * of the JSON payload is read before processing it.
 *
 * Memory-safety notes:
 * - `isRunning` is volatile so that cancel() visibility is guaranteed across
 * threads.
 * - `write()` is synchronized to prevent concurrent writes from corrupting the
 * stream.
 * - `cancel()` uses a guard flag to avoid triggering onConnectionLost() twice.
 * - Constructor closes the InputStream if the OutputStream fails to initialize.
 */
public class BluetoothConnection extends Thread {
    private static final String TAG = "BluetoothConnection";
    private static final int BUFFER_SIZE = 8192;
    private static final byte[] REUSABLE_BUFFER = new byte[BUFFER_SIZE];
    
    private final BluetoothSocket mmSocket;
    private final DataInputStream mmInStream;
    private final DataOutputStream mmOutStream;
    private final ConnectionListener listener;

    // volatile: ensures the cancel() write is immediately visible to the run()
    // thread.
    private volatile boolean isRunning = true;
    // Guard flag: prevents onConnectionLost() from being called more than once.
    private volatile boolean cancelCalled = false;

    // Max allowed payload size (10 MB safeguard to prevent OutOfMemory on malicious
    // or corrupted length prefix)
    private static final int MAX_PAYLOAD_SIZE = 10 * 1024 * 1024;

    public interface ConnectionListener {
        void onMessageReceived(String message);

        void onConnectionLost();
    }

    public BluetoothConnection(BluetoothSocket socket, ConnectionListener listener) {
        mmSocket = socket;
        this.listener = listener;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        DataInputStream tmpDataIn = null;
        DataOutputStream tmpDataOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();

            tmpDataIn = new DataInputStream(tmpIn);
            tmpDataOut = new DataOutputStream(tmpOut);
        } catch (IOException e) {
            CoreLogger.e("BT-CONN: Error occurred when creating streams: " + e.getMessage());
            // If OutputStream fails after InputStream was opened, close the
            // InputStream to avoid leaking the native file descriptor associated with it.
            if (tmpIn != null && tmpOut == null) {
                try {
                    tmpIn.close();
                } catch (IOException ignored) {
                }
            }
        }

        mmInStream = tmpDataIn;
        mmOutStream = tmpDataOut;
    }

    @Override
    public void run() {
        // Guard: if streams failed to initialize, exit immediately.
        if (mmInStream == null) {
            CoreLogger.e("BT-CONN: mmInStream is null, aborting run().");
            notifyConnectionLost();
            return;
        }

        while (isRunning) {
            try {
                // Strict length-prefix framing: read the 4-byte length first.
                int length = mmInStream.readInt();

                if (length > 0 && length <= MAX_PAYLOAD_SIZE) {
                    byte[] buffer = length <= BUFFER_SIZE ? REUSABLE_BUFFER : new byte[length];
                    mmInStream.readFully(buffer, 0, length);

                    String message = new String(buffer, 0, length, "UTF-8");
                    CoreLogger.d("BT-CONN: Message received (" + message.length() + " chars): " +
                            (message.length() > 50 ? message.substring(0, 50) + "..." : message));

                    if (listener != null) {
                        listener.onMessageReceived(message);
                    }
                } else {
                    CoreLogger.e("BT-CONN: Invalid or oversized message length received: " + length);
                    isRunning = false;
                    notifyConnectionLost();
                    break;
                }
            } catch (EOFException e) {
                // End-of-Stream (remote side closed the socket cleanly)
                CoreLogger.d("BT-CONN: Remote closed the connection (EOF).");
                isRunning = false;
                notifyConnectionLost();
                break;
            } catch (IOException e) {
                if (isRunning) {
                    // Only log as an unexpected disconnect if we were not asked to stop.
                    CoreLogger.d("BT-CONN: Stream disconnected unexpectedly: " + e.getMessage());
                } else {
                    CoreLogger.d("BT-CONN: Stream closed after cancel(), ignoring IOException.");
                }
                isRunning = false;
                notifyConnectionLost();
                break;
            }
        }
    }

    /**
     * Send payload to the connected device.
     * Synchronized to prevent concurrent writes from corrupting the stream.
     */
    public synchronized void write(String message) {
        if (!isRunning || mmOutStream == null)
            return;

        try {
            byte[] bytes = message.getBytes("UTF-8");
            CoreLogger.d("BT-CONN: Writing message (" + bytes.length + " bytes)");

            // Strict length-prefix framing: write 4-byte length, then the data.
            mmOutStream.writeInt(bytes.length);
            mmOutStream.write(bytes);
            mmOutStream.flush();
        } catch (IOException e) {
            CoreLogger.e("BT-CONN: Error occurred when sending data: " + e.getMessage());
            isRunning = false;
            notifyConnectionLost();
        }
    }

    /**
     * Notify the listener that the connection was lost — but only once.
     * This avoids double-firing when both write() and run() detect the failure
     * concurrently.
     */
    private synchronized void notifyConnectionLost() {
        if (!cancelCalled && listener != null) {
            cancelCalled = true;
            listener.onConnectionLost();
        }
    }

    /**
     * Call this to shut down the connection cleanly.
     * Sets isRunning=false BEFORE closing the socket so run() knows the close was
     * intentional.
     */
    public void cancel() {
        isRunning = false; // Signal run() loop to stop before throwing IOException
        cancelCalled = true; // Prevent onConnectionLost() from firing on intentional cancel
        try {
            if (mmInStream != null)
                mmInStream.close();
            if (mmOutStream != null)
                mmOutStream.close();
            if (mmSocket != null)
                mmSocket.close();
            CoreLogger.d("BT-CONN: Connection closed cleanly.");
        } catch (IOException e) {
            CoreLogger.e("BT-CONN: Could not close the connect socket: " + e.getMessage());
        }
    }
}
