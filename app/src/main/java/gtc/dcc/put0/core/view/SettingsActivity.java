package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import gtc.dcc.put0.R;
import gtc.dcc.put0.core.utils.AuthUtils;
import gtc.dcc.put0.core.utils.CoreLogger;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.databinding.ActivitySettingsBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modern Settings Activity focused on user transparency and compliance.
 * Handle Firebase Authentication account deletion.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupToolbar();
        setupLegalDisplay();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupLegalDisplay() {
        long timestamp = SharedPreferenceManager.getLegalTimestamp();
        String acceptedVersion = SharedPreferenceManager.getLegalVersion();

        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(timestamp));
            binding.tvLegalInfo.setText(getString(R.string.legal_accepted_at, dateStr, acceptedVersion));
        }
    }

    private void setupClickListeners() {
        binding.btnViewTerms.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_terms_url),
                getString(R.string.accept_terms)));

        binding.btnViewPrivacy.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_privacy_url),
                getString(R.string.accept_privacy)));

        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showLegalDetail(String url, String title) {
        Intent intent = new Intent(this, LegalDetailActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void showDeleteAccountDialog() {
        DialogUtils.showConfirmationDialog(
                this,
                getString(R.string.btn_delete_account),
                getString(R.string.delete_account_confirm),
                "ELIMINAR",
                this::deleteAccount,
                "CANCELAR");
    }

    /**
     * Deletes the user account from Firebase Authentication.
     */
    private void deleteAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        binding.btnDeleteAccount.setEnabled(false);
        binding.btnDeleteAccount.setText("Eliminando...");

        // Delete Firebase Auth account
        currentUser.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        CoreLogger.d("User account deleted successfully");

                        // Clear local data
                        SharedPreferenceManager.clearPreferences();

                        Toast.makeText(this,
                                "Cuenta eliminada exitosamente.",
                                Toast.LENGTH_LONG).show();

                        AuthUtils.signOut(this, LoginActivity.class);
                        finishAffinity();

                    } else {
                        CoreLogger.e(task.getException(), "Failed to delete user account");
                        handleDeletionError(task.getException());
                    }
                });
    }

    private void handleDeletionError(Exception exception) {
        binding.btnDeleteAccount.setEnabled(true);
        binding.btnDeleteAccount.setText(getString(R.string.btn_delete_account));

        if (exception != null && exception.getMessage() != null) {
            if (exception.getMessage().contains("requires-recent-login")) {
                showReauthenticationDialog();
                return;
            }
        }

        Toast.makeText(this, "Error al eliminar la cuenta. Por favor, intenta nuevamente.", Toast.LENGTH_LONG).show();
    }

    private void showReauthenticationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación de Seguridad")
                .setMessage("Por seguridad, debes volver a iniciar sesión antes de eliminar tu cuenta.")
                .setPositiveButton("INICIAR SESIÓN", (dialog, which) -> {
                    AuthUtils.signOut(this, LoginActivity.class);
                    finishAffinity();
                })
                .setNegativeButton("CANCELAR", null)
                .setCancelable(false)
                .show();
    }
}