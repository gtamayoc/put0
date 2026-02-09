package gtc.dcc.put0.core.view;

import android.view.View;

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

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // WebView configuration
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.loadUrl(url);

        // Scroll detection to enable button
        binding.nestedScrollView
                .setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX,
                        scrollY, oldScrollX, oldScrollY) -> {
                    // Check if scroll reached bottom (with a small buffer for better UX)
                    View child = v.getChildAt(0);
                    if (child != null) {
                        int diff = (child.getMeasuredHeight() - v.getMeasuredHeight());
                        if (scrollY >= diff - 50) { // 50px buffer to appear immediately
                            if (binding.btnAcceptDetail.getVisibility() != View.VISIBLE) {
                                binding.btnAcceptDetail.setVisibility(View.VISIBLE);
                                // Hide the info message when reached bottom
                                binding.cardInfo.setVisibility(View.GONE);
                            }
                        }
                    }
                });

        binding.btnAcceptDetail.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}
