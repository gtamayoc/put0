package gtc.dcc.put0.core.view;

import android.os.Bundle;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import gtc.dcc.put0.databinding.ActivityLegalDetailBinding;

/**
 * Activity to display the full text of legal documents using a WebView.
 */
public class LegalDetailActivity extends AppCompatActivity {

    private ActivityLegalDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLegalDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");

        // UI Setup
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // WebView configuration
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.getSettings().setJavaScriptEnabled(true); // Needed if the hosting requires it
        binding.webView.loadUrl(url);
    }
}
