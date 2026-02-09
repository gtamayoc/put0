package gtc.dcc.put0.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import gtc.dcc.put0.core.utils.CoreLogger;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;

public class SharedPreferenceManager {
    private static final String PREFS_NAME = "gtc.dcc.put0.PREFS";
    private static final String TOKEN_KEY = "auth_token";
    private static final String LEGAL_ACCEPTED_KEY = "legal_accepted";
    private static final String LEGAL_VERSION_KEY = "legal_version";
    private static final String LEGAL_TIMESTAMP_KEY = "legal_timestamp";
    private static SharedPreferences sharedPreferences;
    private static Gson gson = new Gson(); // Declarado como estático para ser utilizado sin necesidad de instanciar

    // Método para inicializar EncryptedSharedPreferences (se debe llamar en la
    // clase Application o al iniciar la app)
    public static void initialize(Context context) {
        if (sharedPreferences == null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                sharedPreferences = EncryptedSharedPreferences.create(
                        PREFS_NAME, // Nombre del archivo de preferencias
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (GeneralSecurityException | IOException e) {
                CoreLogger.e(e,
                        "Error initializing EncryptedSharedPreferences. Falling back to standard SharedPreferences.");
                sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
    }

    // Direct String methods to avoid Gson for simple configs
    public static void saveString(String key, String value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences != null ? sharedPreferences.getString(key, defaultValue) : defaultValue;
    }

    // Método para guardar el token
    public static void setToken(String authToken) {
        saveString(TOKEN_KEY, authToken);
    }

    // Método para obtener el token
    public static String getToken() {
        return sharedPreferences != null ? sharedPreferences.getString(TOKEN_KEY, null) : null;
    }

    // Método para eliminar el token (en caso de cerrar sesión, por ejemplo)
    public static void clearToken() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(TOKEN_KEY).apply();
        }
    }

    // Legal Acceptance Methods
    public static boolean isLegalAccepted() {
        return sharedPreferences != null && sharedPreferences.getBoolean(LEGAL_ACCEPTED_KEY, false);
    }

    public static void setLegalAccepted(boolean accepted) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(LEGAL_ACCEPTED_KEY, accepted).apply();
        }
    }

    public static String getLegalVersion() {
        return sharedPreferences != null ? sharedPreferences.getString(LEGAL_VERSION_KEY, "") : "";
    }

    public static void setLegalVersion(String version) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(LEGAL_VERSION_KEY, version).apply();
        }
    }

    public static long getLegalTimestamp() {
        return sharedPreferences != null ? sharedPreferences.getLong(LEGAL_TIMESTAMP_KEY, 0L) : 0L;
    }

    public static void setLegalTimestamp(long timestamp) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putLong(LEGAL_TIMESTAMP_KEY, timestamp).apply();
        }
    }

    // Guardar un objeto genérico
    public static <T> void saveObject(String key, T object) {
        if (sharedPreferences != null) {
            String json = new Gson().toJson(object);
            sharedPreferences.edit().putString(key, json).apply();
        }
    }

    // Recuperar un objeto genérico
    public static <T> T getObject(String key, Class<T> clazz) {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(key, null);
            return json != null ? new Gson().fromJson(json, clazz) : null;
        }
        return null;
    }

    // Recuperar una lista de objetos genéricos
    public static <T> T getObjectList(String key, Type typeOfT) {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(key, null);
            return json != null ? new Gson().fromJson(json, typeOfT) : null;
        }
        return null;
    }

    // Limpiar todas las preferencias
    public static void clearPreferences() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().apply();
        }
    }

    // Método para guardar cualquier tipo de dato (clave dinámica)
    public static <T> void saveData(String key, T value) {
        if (sharedPreferences != null) {
            String json = gson.toJson(value);
            sharedPreferences.edit().putString(key, json).apply();
        }
    }

    // Método para obtener datos genéricos basados en la clave
    public static <T> T getData(String key, Class<T> clazz) {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(key, null);
            return json != null ? gson.fromJson(json, clazz) : null;
        }
        return null;
    }

    // Método para obtener datos genéricos con valor predeterminado
    public static <T> T getData(String key, Class<T> clazz, T defaultValue) {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(key, null);
            return json != null ? gson.fromJson(json, clazz) : defaultValue;
        }
        return defaultValue;
    }

    // Método para obtener datos en formato de lista
    public static <T> T getDataList(String key, Type typeOfT) {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(key, null);
            return json != null ? gson.fromJson(json, typeOfT) : null;
        }
        return null;
    }

    // Método para eliminar un dato (por ejemplo, al cerrar sesión o borrar un dato)
    public static void removeData(String key) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

}
