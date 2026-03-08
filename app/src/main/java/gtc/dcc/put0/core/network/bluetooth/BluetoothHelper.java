package gtc.dcc.put0.core.network.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import gtc.dcc.put0.core.utils.CoreLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper for Bluetooth operations. Compatible with Android API 23 (minSdk)
 * through 35 (targetSdk / Android 15).
 *
 * Permission matrix:
 * API 23-30: BLUETOOTH + BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION (required for
 * BT discovery)
 * API 31+ : BLUETOOTH_SCAN (neverForLocation) + BLUETOOTH_CONNECT +
 * BLUETOOTH_ADVERTISE
 *
 * Android 15 notes:
 * • BLUETOOTH_CONNECT is required BEFORE calling BluetoothAdapter.isEnabled()
 * on API 31+.
 * Without it a SecurityException is thrown immediately.
 * • Insecure RFCOMM sockets may be rejected by the stricter Android 15
 * Bluetooth stack
 * if the devices are not bonded. The client service falls back to secure
 * sockets.
 */
public class BluetoothHelper {

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothHelper(Context context) {
        BluetoothAdapter adapter = null;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            adapter = bluetoothManager.getAdapter();
        }
        // Fallback for rare cases where BluetoothManager returns null
        if (adapter == null) {
            // noinspection deprecation
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        this.bluetoothAdapter = adapter;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Checks whether Bluetooth is currently enabled.
     *
     * IMPORTANT: On Android 12+ (API 31+) calling BluetoothAdapter.isEnabled()
     * without
     * BLUETOOTH_CONNECT permission immediately throws a SecurityException. This
     * method
     * checks the permission first so callers don't need to worry about it.
     *
     * @return true if BT is enabled AND all required permissions are granted.
     */
    public boolean isBluetoothEnabled(Context context) {
        if (bluetoothAdapter == null) {
            CoreLogger.w("BT-HELP: Bluetooth adapter is null");
            return false;
        }
        // API 31+: BLUETOOTH_CONNECT is required to even call isEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                CoreLogger.w("BT-HELP: BLUETOOTH_CONNECT not granted — cannot check isEnabled()");
                return false;
            }
        }
        try {
            return bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            CoreLogger.e("BT-HELP: SecurityException calling isEnabled(): " + e.getMessage());
            return false;
        }
    }

    /**
     * @deprecated Use {@link #isBluetoothEnabled(Context)} which is safe on API
     *             31+.
     */
    @Deprecated
    public boolean isBluetoothEnabled() {
        if (bluetoothAdapter == null)
            return false;
        try {
            return bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            CoreLogger.e("BT-HELP: SecurityException in legacy isBluetoothEnabled(): " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the complete set of runtime permissions required for this app's
     * Bluetooth multiplayer feature, based on the running API level.
     *
     * API 31+ requires all three granular Bluetooth permissions:
     * • BLUETOOTH_SCAN — to discover / enumerate bonded devices
     * • BLUETOOTH_CONNECT — to open RFCOMM sockets and call isEnabled()
     * • BLUETOOTH_ADVERTISE— to expose the host's RFCOMM server socket
     */
    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): all three granular permissions are needed
            return new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            };
        } else {
            // Android 6-11 (API 23-30): Location IS required for BT discovery
            return new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    /**
     * Returns true only if the BLUETOOTH_CONNECT permission is granted.
     * This is the single most important permission on API 31+ — without it
     * nearly every BluetoothAdapter / BluetoothSocket call throws
     * SecurityException.
     */
    public static boolean hasConnectPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        // API < 31: no explicit BLUETOOTH_CONNECT needed
        return true;
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                CoreLogger.w("BT-HELP: Missing permission: " + permission);
                return false;
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity, int requestCode) {
        String[] perms = getRequiredPermissions();
        if (perms.length > 0) {
            ActivityCompat.requestPermissions(activity, perms, requestCode);
        }
    }

    /**
     * Returns the list of currently bonded (paired) devices.
     * Requires BLUETOOTH_CONNECT on API 31+ — already guarded via
     * hasAllPermissions().
     * The @SuppressLint is intentional; permission is verified before the call.
     */
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices(Context context) {
        List<BluetoothDevice> result = new ArrayList<>();
        if (bluetoothAdapter == null) {
            CoreLogger.w("BT-HELP: Bluetooth adapter is null");
            return result;
        }
        if (!hasAllPermissions(context)) {
            CoreLogger.w("BT-HELP: Missing permissions for getPairedDevices");
            return result;
        }
        try {
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded != null) {
                result.addAll(bonded);
                CoreLogger.d("BT-HELP: " + result.size() + " paired devices found");
            }
        } catch (SecurityException e) {
            CoreLogger.e("BT-HELP: SecurityException in getBondedDevices: " + e.getMessage());
        }
        return result;
    }
}
