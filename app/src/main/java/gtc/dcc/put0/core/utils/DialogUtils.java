package gtc.dcc.put0.core.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import gtc.dcc.put0.R;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static void showExitConfirmationDialog(Activity activity, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(activity, R.style.CustomAlertDialogTheme)
                .setTitle("Confirmación de salida")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    onConfirm.run();
                    activity.finish();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showConfirmationDialog(Activity activity,
            String title,
            String message,
            String positiveButtonText,
            Runnable onConfirm,
            String negativeButtonText) {
        new MaterialAlertDialogBuilder(activity, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    onConfirm.run();
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showGameFormDialog(Activity activity, OnGameFormSubmitListener listener) {
        // Use BottomSheetDialog for better ergonomics
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_game_form, null);
        bottomSheetDialog.setContentView(dialogView);

        // Find views using findViewById
        TextInputEditText gameNameInput = dialogView.findViewById(R.id.edit_game_name);
        Spinner minPlayersSpinner = dialogView.findViewById(R.id.spinner_min_players);
        Spinner maxPlayersSpinner = dialogView.findViewById(R.id.spinner_max_players);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btn_submit);

        // Configure Spinners
        Integer[] playerOptions = { 2, 3, 4, 5, 6 };
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                playerOptions);
        minPlayersSpinner.setAdapter(adapter);
        maxPlayersSpinner.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String gameName = "";
            if (gameNameInput.getText() != null) {
                gameName = gameNameInput.getText().toString().trim();
            }

            int minPlayers = (int) minPlayersSpinner.getSelectedItem();
            int maxPlayers = (int) maxPlayersSpinner.getSelectedItem();

            if (gameName.isEmpty() || minPlayers <= 0 || maxPlayers <= 0 || minPlayers > maxPlayers) {
                Toast.makeText(activity, "Por favor, ingresa un nombre válido y verifica los rangos de jugadores",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onSubmit(gameName, minPlayers, maxPlayers);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    public interface OnGameFormSubmitListener {
        void onSubmit(String gameName, int minPlayers, int maxPlayers);
    }

    public interface OnModeSelectedListener {
        void onModeSelected(gtc.dcc.put0.core.data.model.MatchMode mode);
    }

    public static void showModeSelectionDialog(Activity activity, OnModeSelectedListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_mode_selection, null);
        bottomSheetDialog.setContentView(view);

        MaterialCardView cardSoloBot = view.findViewById(R.id.cardSoloBot);
        // MaterialCardView cardSoloAmigo = view.findViewById(R.id.cardSoloAmigo); //
        // Assuming ID exists or removing if not in layout

        if (cardSoloBot != null) {
            cardSoloBot.setOnClickListener(v -> {
                listener.onModeSelected(gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT);
                bottomSheetDialog.dismiss();
            });
        }

        // Logic for other cards...
        View cardSoloAmigo = view.findViewById(R.id.cardSoloAmigo);
        if (cardSoloAmigo != null) {
            cardSoloAmigo.setOnClickListener(v -> {
                listener.onModeSelected(gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_AMIGO);
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    public interface OnServerIpConfiguredListener {
        void onIpConfigured(String ipAddress);
    }

    public static void showServerIpDialog(Activity activity, OnServerIpConfiguredListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_server_ip, null);
        bottomSheetDialog.setContentView(view);

        TextInputEditText input = view.findViewById(R.id.etServerIp);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        String currentIp = SharedPreferenceManager.getString("server_ip", "10.0.2.2:8080");
        if (input != null) {
            input.setText(currentIp.replace("\"", "").trim());
        }

        btnSave.setOnClickListener(v -> {
            if (input != null && input.getText() != null) {
                String ip = input.getText().toString().trim();
                if (!ip.isEmpty()) {
                    listener.onIpConfigured(ip);
                    bottomSheetDialog.dismiss();
                }
            }
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.cancel());

        bottomSheetDialog.show();
    }
}