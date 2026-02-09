package gtc.dcc.put0.core.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * General utility class for application-level metadata.
 */
public class AppUtils {
    /**
     * Current version of the legal terms (Privacy Policy and Terms of Use).
     * Update this value only when the legal content changes to trigger new
     * acceptance.
     */
    public static final String CURRENT_LEGAL_VERSION = "1.0.0";

    /**
     * Retrieves the version name of the application as defined in build.gradle.
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName != null ? packageInfo.versionName : "unknown";
        } catch (PackageManager.NameNotFoundException e) {
            CoreLogger.e(e, "Error getting package version name");
            return "unknown";
        }
    }
}
