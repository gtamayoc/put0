package gtc.dcc.put0.core.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogBehavior;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.bottomsheets.BottomSheet;

import gtc.dcc.put0.R;


public final class DialogUtils {
    private DialogUtils() {
    }

    public static void showExitConfirmationDialog(Activity activity, Runnable onConfirm) {
        new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                .setTitle("Confirmación de salida")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    onConfirm.run();
                    activity.finish();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Método estático para mostrar un diálogo de confirmación genérico
    public static void showConfirmationDialog(Activity activity,
                                              String title,
                                              String message,
                                              String positiveButtonText,
                                              Runnable onConfirm,
                                              String negativeButtonText) {
        new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    onConfirm.run(); // Ejecuta la acción de confirmación
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss()) // Cierra el diálogo
                .show();
    }

    public static void showGameFormDialog(Activity activity, OnGameFormSubmitListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_game_form, null);

        if (dialogView == null) {
            throw new IllegalStateException("Error inflating dialog_game_form.xml");
        }

        // Busca los elementos del diálogo
        EditText gameNameInput = dialogView.findViewById(R.id.edit_game_name);
        Spinner minPlayersSpinner = dialogView.findViewById(R.id.spinner_min_players);
        Spinner maxPlayersSpinner = dialogView.findViewById(R.id.spinner_max_players);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit);

        // Configurar los Spinners
        Integer[] playerOptions = {2, 3, 4, 5, 6}; // Opciones predefinidas
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, playerOptions);
        minPlayersSpinner.setAdapter(adapter);
        maxPlayersSpinner.setAdapter(adapter);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String gameName = gameNameInput.getText().toString().trim();
            int minPlayers = (int) minPlayersSpinner.getSelectedItem();
            int maxPlayers = (int) maxPlayersSpinner.getSelectedItem();

            if (gameName.isEmpty() || minPlayers <= 0 || maxPlayers <= 0 || minPlayers > maxPlayers) {
                Toast.makeText(activity, "Por favor, ingresa un nombre válido y verifica los rangos de jugadores", Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onSubmit(gameName, minPlayers, maxPlayers);
            dialog.dismiss();
        });

        dialog.show();
    }


    public interface OnGameFormSubmitListener {
        void onSubmit(String gameName, int minPlayers, int maxPlayers);
    }

}