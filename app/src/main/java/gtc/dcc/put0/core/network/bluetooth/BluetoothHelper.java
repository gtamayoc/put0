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
 * through 35 (targetSdk).
 *
 * Permission matrix:
 * API 23-30: BLUETOOTH + BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION (required for
 * device discovery)
 * API 31+ : BLUETOOTH_SCAN (neverForLocation) + BLUETOOTH_CONNECT
 */
public class BluetoothHelper {

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothHelper(Context context) {
        BluetoothAdapter adapter = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                adapter = bluetoothManager.getAdapter();
            }
        }
        // Fallback for older APIs or if manager returned null
        if (adapter == null) {
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

    public boolean isBluetoothEnabled() {
        if (bluetoothAdapter == null) {
            CoreLogger.w("BT-HELP: Bluetooth adapter is null");
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Returns the runtime permissions required based on API level.
     */
    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): New granular Bluetooth permissions
            return new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
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
     * The @SuppressLint is intentional — permission is already checked via
     * hasAllPermissions().
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
