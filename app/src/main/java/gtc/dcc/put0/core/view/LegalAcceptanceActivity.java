package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import gtc.dcc.put0.R;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.databinding.ActivityLegalAcceptanceBinding;

/**
 * Activity for the initial legal acceptance flow (Terms and Privacy Policy).
 * Required for compliance with Colombian Law 1581 and Google Play policies.
 */
public class LegalAcceptanceActivity extends AppCompatActivity {

    private ActivityLegalAcceptanceBinding binding;
    private androidx.activity.result.ActivityResultLauncher<Intent> termsLauncher;
    private androidx.activity.result.ActivityResultLauncher<Intent> privacyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLegalAcceptanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupLaunchers();
        setupListeners();
        handleUpdateUI();
    }

    private void setupLaunchers() {
        termsLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        binding.cbTerms.setChecked(true);
                        hideError();
                    } else {
                        showError();
                    }
                });

        privacyLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        binding.cbPrivacy.setChecked(true);
                        hideError();
                    } else {
                        showError();
                    }
                });
    }

    private void handleUpdateUI() {
        boolean isUpdate = getIntent().getBooleanExtra("is_update", false);
        if (isUpdate) {
            binding.tvTitle.setText(R.string.legal_update_title);
            binding.tvDescription.setText(R.string.legal_update_desc);
            // Mostrar un rayo o algo que indique cambio/novedad
            binding.ivLegalIcon.setImageResource(R.drawable.ic_lightning);
        }
    }

    private void setupListeners() {
        // Enforce explicit clickwrap acceptance
        binding.cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> validateAcceptance());
        binding.cbPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> validateAcceptance());

        // View detail links
        binding.tvTermsLink.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_terms_url),
                getString(R.string.accept_terms),
                termsLauncher));

        binding.tvPrivacyLink.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_privacy_url),
                getString(R.string.accept_privacy),
                privacyLauncher));

        // Handle continuation and persistence
        binding.btnContinue.setOnClickListener(v -> {
            String legalVersion = gtc.dcc.put0.core.utils.AppUtils.CURRENT_LEGAL_VERSION;
            SharedPreferenceManager.setLegalAccepted(true);
            SharedPreferenceManager.setLegalVersion(legalVersion);
            SharedPreferenceManager.setLegalTimestamp(System.currentTimeMillis());

            // Proceed to standard onboarding/login flow
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void validateAcceptance() {
        boolean accepted = binding.cbTerms.isChecked() && binding.cbPrivacy.isChecked();
        binding.btnContinue.setEnabled(accepted);
        if (accepted)
            hideError();
    }

    private void showError() {
        binding.cardLegal.setStrokeColor(getResources().getColor(R.color.errorDark));
    }

    private void hideError() {
        binding.cardLegal.setStrokeColor(android.graphics.Color.parseColor("#33FFFFFF"));
    }

    private void showLegalDetail(String url, String title,
            androidx.activity.result.ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(this, LegalDetailActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        launcher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing the screen
        finishAffinity();
    }
}
