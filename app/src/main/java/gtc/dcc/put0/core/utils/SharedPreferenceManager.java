package gtc.dcc.put0.core.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;

public class SharedPreferenceManager {
    private static final String PREFS_NAME = "gtc.dcc.put0.PREFS";
    private static final String TOKEN_KEY = "auth_token";
    private static SharedPreferences sharedPreferences;
    private static Gson gson = new Gson();  // Declarado como estático para ser utilizado sin necesidad de instanciar


    // Método para inicializar EncryptedSharedPreferences (se debe llamar en la clase Application o al iniciar la app)
    public static void initialize(Context context) {
        if (sharedPreferences == null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                sharedPreferences = EncryptedSharedPreferences.create(
                        PREFS_NAME, // Nombre del archivo de preferencias
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                Log.e("PUTO", "Error al inicializar preferencias cifradas" + e);
            }
        }
    }

    // Método para guardar el token
    public static void setToken(String authToken) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(TOKEN_KEY, authToken).apply();
        }
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
