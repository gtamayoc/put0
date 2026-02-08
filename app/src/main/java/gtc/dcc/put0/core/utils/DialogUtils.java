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
        void onModeSelected(gtc.dcc.put0.core.data.model.MatchMode mode, int deckSize);
    }

    public static void showModeSelectionDialog(Activity activity, OnModeSelectedListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_mode_selection, null);
        bottomSheetDialog.setContentView(view);

        // Force bottom sheet to be fully expanded
        bottomSheetDialog.getBehavior()
                .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.getBehavior().setSkipCollapsed(true);

        // Transparent background to show rounded corners
        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet)
                    .setBackgroundResource(android.R.color.transparent);
        }

        // Deck Options (LinearLayouts acting as cards)
        View cardDeck52 = view.findViewById(R.id.cardDeck52);
        View cardDeck104 = view.findViewById(R.id.cardDeck104);
        android.widget.TextView tvDeck52Title = view.findViewById(R.id.tvDeck52Title);
        android.widget.TextView tvDeck104Title = view.findViewById(R.id.tvDeck104Title);

        // Mode Options
        View cardSoloBot = view.findViewById(R.id.cardSoloBot);
        View cardSoloAmigo = view.findViewById(R.id.cardSoloAmigo);

        MaterialButton btnStartGame = view.findViewById(R.id.btnStartGame);

        // State holder
        final int[] selectedDeckSize = { 52 }; // Default 52
        final gtc.dcc.put0.core.data.model.MatchMode[] selectedMode = {
                gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT }; // Default Bot

        // -- Helpers to update UI --

        Runnable updateDeckUI = () -> {
            if (cardDeck52 == null || cardDeck104 == null)
                return;
            // Selected Deck gets Purple Border (bg_card_selected) and Title is Purple
            // Unselected gets No Border (bg_card_unselected) and Title is White

            if (selectedDeckSize[0] == 52) {
                cardDeck52.setBackgroundResource(R.drawable.bg_card_selected);
                if (tvDeck52Title != null)
                    tvDeck52Title.setTextColor(activity.getResources().getColor(R.color.brand_green));

                cardDeck104.setBackgroundResource(R.drawable.bg_card_unselected);
                if (tvDeck104Title != null)
                    tvDeck104Title.setTextColor(0xFFFFFFFF);
            } else {
                cardDeck104.setBackgroundResource(R.drawable.bg_card_selected);
                if (tvDeck104Title != null)
                    tvDeck104Title.setTextColor(activity.getResources().getColor(R.color.brand_green));

                cardDeck52.setBackgroundResource(R.drawable.bg_card_unselected);
                if (tvDeck52Title != null)
                    tvDeck52Title.setTextColor(0xFFFFFFFF);
            }
        };

        Runnable updateModeUI = () -> {
            if (cardSoloBot == null || cardSoloAmigo == null)
                return;
            boolean isBot = selectedMode[0] == gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT;

            // Selected Mode gets Purple Border (bg_card_selected)
            // Unselected gets (bg_card_unselected)
            if (isBot) {
                cardSoloBot.setBackgroundResource(R.drawable.bg_card_selected);
                cardSoloAmigo.setBackgroundResource(R.drawable.bg_card_unselected);
            } else {
                cardSoloAmigo.setBackgroundResource(R.drawable.bg_card_selected);
                cardSoloBot.setBackgroundResource(R.drawable.bg_card_unselected);
            }
        };

        // -- Listeners --

        if (cardDeck52 != null) {
            cardDeck52.setOnClickListener(v -> {
                selectedDeckSize[0] = 52;
                updateDeckUI.run();
            });
        }

        if (cardDeck104 != null) {
            cardDeck104.setOnClickListener(v -> {
                selectedDeckSize[0] = 104;
                updateDeckUI.run();
            });
        }

        if (cardSoloBot != null) {
            cardSoloBot.setOnClickListener(v -> {
                selectedMode[0] = gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT;
                updateModeUI.run();
            });
        }

        if (cardSoloAmigo != null) {
            cardSoloAmigo.setOnClickListener(v -> {
                selectedMode[0] = gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_AMIGO;
                updateModeUI.run();
            });
        }

        if (btnStartGame != null) {
            btnStartGame.setOnClickListener(v -> {
                if (selectedMode[0] != null) {
                    listener.onModeSelected(selectedMode[0], selectedDeckSize[0]);
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(activity, "Selecciona un modo de juego", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Initialize UI
        updateDeckUI.run();
        updateModeUI.run();

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