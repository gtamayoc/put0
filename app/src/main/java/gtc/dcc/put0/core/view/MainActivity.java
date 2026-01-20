package gtc.dcc.put0.core.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.bottomsheets.BottomSheet;
import gtc.dcc.put0.core.utils.CoreLogger;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.utils.AuthUtils;
import gtc.dcc.put0.core.utils.CodeGenerator;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.NavigationUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.viewmodel.MainViewModel;
import gtc.dcc.put0.core.viewmodel.UserViewModel;
import gtc.dcc.put0.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private UserViewModel userViewModel;
    private gtc.dcc.put0.core.viewmodel.GameViewModel gameViewModel;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadCompatibility();
        initializeViewModels();
        setupObservers();
        setupClickListeners();
        AuthUtils.initializeGoogleSignIn(this, getString(R.string.default_web_client_id));
        loadUserProfile();
    }

    private void loadUserProfile() {
        // Load current user's profile from Google Sign-In
        com.google.android.gms.auth.api.signin.GoogleSignInAccount account = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(this);

        if (account != null) {
            String email = account.getEmail();
            android.net.Uri photoUri = account.getPhotoUrl();
            String photoUrl = photoUri != null ? photoUri.toString() : null;
            updateUserInfo(email, photoUrl);
        }
    }

    private void loadCompatibility() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        gameViewModel = new ViewModelProvider(this).get(gtc.dcc.put0.core.viewmodel.GameViewModel.class);
    }

    private void setupObservers() {
        userViewModel.getResponseDetailsGetUser().observe(this, this::handleUserResponse);
        viewModel.selectedActivity.observe(this, activity -> {
            NavigationUtils.navigateToNextFinis(this, activity);
        });

        // Observe Game Creation
        gameViewModel.getCurrentGameId().observe(this, gameId -> {
            if (gameId != null && !gameId.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(this, LobbyActivity.class);
                intent.putExtra("GAME_ID", gameId);
                // intent.putExtra("MATCH_MODE", selectedMode); // Ideally pass this too
                startActivity(intent);
            }
        });

        gameViewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.ivProfilePhoto.setOnClickListener(v -> {
            DialogUtils.showServerIpDialog(this, ipAddress -> {
                SharedPreferenceManager.saveString("server_ip", ipAddress);
                gtc.dcc.put0.core.data.remote.ApiClient.resetApiClient();
                Toast.makeText(this, "IP Updated. Reconnecting...", Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnCreateGame.setOnClickListener(v -> {
            DialogUtils.showModeSelectionDialog(this, mode -> {
                String userName = SharedPreferenceManager.getData("user_name", String.class, "Player");
                // Logic based on mode
                int botCount = 0;
                if (mode == gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT) {
                    botCount = 1; // Default 1 bot
                }

                // Show form only if needed (e.g., custom names or settings), else direct create
                // For simplicity, direct create for now and verify
                gameViewModel.createGame(userName, botCount, mode);
                Toast.makeText(this, "Creating " + mode + "...", Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnJoinGame.setOnClickListener(v -> {
            viewModel.onJoinGameClicked();
        });
        binding.btnSettings.setOnClickListener(v -> viewModel.onSettingsClicked());
        binding.btnLogout.setOnClickListener(v -> showExitDialog());
    }

    private void updateUserInfo(String email, String imageUrl) {
        userViewModel.getUserByEmail(email);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.ivProfilePhoto);
        }
        CoreLogger.d("User email: " + email);
    }

    private void handleUserResponse(ResponseDetails response) {
        if (response == null) {
            handleSignOutError();
            return;
        }

        if (response.getData() instanceof User) {
            User user = (User) response.getData();
            saveUserData(user);
        }
    }

    private void handleSignOutError() {
        Toast.makeText(this, "General error RegisterGoogle.", Toast.LENGTH_LONG).show();
    }

    private void saveUserData(User user) {
        SharedPreferenceManager.saveData("user_id", user.getId());
        SharedPreferenceManager.saveData("user_rol", user.getRol());
        SharedPreferenceManager.saveData("user_name", user.getNames());
    }

    private void showExitDialog() {
        DialogUtils.showExitConfirmationDialog(this, () -> AuthUtils.signOut(this, LoginActivity.class));
    }
}