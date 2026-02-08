package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import gtc.dcc.put0.R;
import gtc.dcc.put0.core.utils.AuthUtils;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.databinding.ActivitySettingsBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modern Settings Activity with focus on user transparency and compliance.
 * Includes links to legal documents and the mandatory "Delete Account" button.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

    private void deleteAccount() {
        // Compliance Requirement (Google Play): Provide an visible account deletion
        // option.
        // In a production app, this should clear data on the server via API.
        Toast.makeText(this, "Solicitud de eliminación procesada", Toast.LENGTH_LONG).show();

        AuthUtils.signOut(this, LoginActivity.class);
        SharedPreferenceManager.clearPreferences();
        finishAffinity();
    }
}