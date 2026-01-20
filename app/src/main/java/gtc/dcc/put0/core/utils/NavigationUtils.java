package gtc.dcc.put0.core.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import gtc.dcc.put0.core.utils.CoreLogger;

public final class NavigationUtils {
    private NavigationUtils() {
    } // Prevenir instanciación

    /**
     * Navega a la siguiente Activity
     * 
     * @param context     Contexto actual
     * @param targetClass Clase de la Activity destino
     */
    public static void navigateToNextFinis(Activity context, Class<?> targetClass) {
        CoreLogger.d("Navigating to: " + targetClass.getName());
        Intent intent = new Intent(context, targetClass);
        context.startActivity(intent);
    }

    /**
     * Navega a la siguiente Activity y finaliza la actual
     * 
     * @param context     Contexto actual
     * @param targetClass Clase de la Activity destino
     */
    public static void navigateToNext(Activity context, Class<?> targetClass) {
        CoreLogger.d("Navigating to: " + targetClass.getName());
        Intent intent = new Intent(context, targetClass);
        context.startActivity(intent);
        context.finish();
    }

    /**
     * Navega a la siguiente Activity con extras
     */
    public static void navigateToNextWithExtras(Activity context, Class<?> targetClass, Bundle extras) {
        Intent intent = new Intent(context, targetClass);
        intent.putExtras(extras);
        context.startActivity(intent);
        context.finish();
    }
}