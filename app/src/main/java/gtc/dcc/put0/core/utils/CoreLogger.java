package gtc.dcc.put0.core.utils;

import com.orhanobut.logger.Logger;

/**
 * Centralized Logging utility for the application.
 * Wraps the underlying logging library to provide a consistent interface
 * and allow for global configuration/interception.
 */
public class CoreLogger {

    public static void d(String message, Object... args) {
        Logger.d(message, args);
    }

    public static void d(Object object) {
        Logger.d(object);
    }

    public static void e(String message, Object... args) {
        Logger.e(message, args);
    }

    public static void e(Throwable throwable, String message, Object... args) {
        Logger.e(throwable, message, args);
    }

    public static void w(String message, Object... args) {
        Logger.w(message, args);
    }

    public static void i(String message, Object... args) {
        Logger.i(message, args);
    }

    public static void v(String message, Object... args) {
        Logger.v(message, args);
    }

    public static void json(String json) {
        Logger.json(json);
    }
}
