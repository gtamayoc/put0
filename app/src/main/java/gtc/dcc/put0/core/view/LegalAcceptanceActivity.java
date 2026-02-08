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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLegalAcceptanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        // Enforce explicit clickwrap acceptance
        binding.cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> validateAcceptance());
        binding.cbPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> validateAcceptance());

        // View detail links
        binding.tvTermsLink.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_terms_url),
                getString(R.string.accept_terms)));

        binding.tvPrivacyLink.setOnClickListener(v -> showLegalDetail(
                getString(R.string.legal_privacy_url),
                getString(R.string.accept_privacy)));

        // Handle continuation and persistence
        binding.btnContinue.setOnClickListener(v -> {
            String currentAppVersion = gtc.dcc.put0.core.utils.AppUtils.getAppVersionName(this);
            SharedPreferenceManager.setLegalAccepted(true);
            SharedPreferenceManager.setLegalVersion(currentAppVersion);
            SharedPreferenceManager.setLegalTimestamp(System.currentTimeMillis());

            // Proceed to standard onboarding/login flow
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void validateAcceptance() {
        binding.btnContinue.setEnabled(binding.cbTerms.isChecked() && binding.cbPrivacy.isChecked());
    }

    private void showLegalDetail(String url, String title) {
        Intent intent = new Intent(this, LegalDetailActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing the screen
        finishAffinity();
    }
}
