package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import gtc.dcc.put0.core.utils.GameMessageHelper;

import gtc.dcc.put0.core.data.model.GameStatus;
import gtc.dcc.put0.core.data.model.MatchMode;
import gtc.dcc.put0.core.viewmodel.GameViewModel;
import gtc.dcc.put0.databinding.ActivityLobbyBinding;

public class LobbyActivity extends AppCompatActivity {

    private ActivityLobbyBinding binding;
    private GameViewModel viewModel;
    private MatchMode currentMode;
    private String gameId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        initializeViewModel();
        handleIntentData();
        setupUI();
        setupObservers();
        startLoadingProcess();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void startLoadingProcess() {
        // Show loading screen for 4 seconds to simulate prep and ensure server sync
        binding.clLoadingOverlay.setVisibility(View.VISIBLE);
        binding.clLoadingOverlay.postDelayed(() -> {
            binding.clLoadingOverlay.setVisibility(View.GONE);
        }, 4000);
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
    }

    private void handleIntentData() {
        if (getIntent().hasExtra("GAME_ID")) {
            gameId = getIntent().getStringExtra("GAME_ID");
            // Trigger viewModel to load/observe this game
            // viewModel.joinRoom(gameId, "CurrentPlayer"); // Assuming already joined or
            // created?
            // If created, we might already have data in Repository, but good to
            // refresh/subscribe
        }

        if (getIntent().hasExtra("MATCH_MODE")) {
            currentMode = (MatchMode) getIntent().getSerializableExtra("MATCH_MODE");
        } else {
            currentMode = MatchMode.SOLO_VS_BOT; // Default fallback
        }
    }

    private void setupUI() {
        binding.tvGameCode
                .setText("CODE: " + (gameId != null ? gameId.substring(0, Math.min(gameId.length(), 6)) : "WAITING"));
        binding.tvModeInfo.setText("Mode: " + currentMode.name());

        switch (currentMode) {
            case SOLO_VS_BOT:
                binding.btnStartGame.setVisibility(View.VISIBLE);
                binding.btnShareCode.setVisibility(View.GONE);
                binding.tvPlayerCount.setVisibility(View.GONE);
                binding.rvLobbyPlayers.setVisibility(View.GONE); // Hide player list in solo

                // Adjust constraints or weight if needed, but for now just hiding is enough
                // to simplify the view as requested.
                binding.llInfoContainer
                        .setLayoutParams(new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                0, 0));
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.llInfoContainer
                        .getLayoutParams();
                lp.matchConstraintPercentWidth = 1.0f; // Full width for info
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToBottom = binding.tvLobbyTitle.getId();
                lp.bottomToTop = binding.llActionButtons.getId();
                binding.llInfoContainer.setLayoutParams(lp);
                break;
            case SOLO_VS_AMIGO:
                binding.btnStartGame.setEnabled(false); // Enable only when 2 players
                binding.btnStartGame.setText("WAITING FOR PLAYER...");
                binding.btnShareCode.setVisibility(View.VISIBLE);
                break;
            case SOLO_VS_AMIGOS:
                binding.btnStartGame.setVisibility(View.VISIBLE);
                binding.btnShareCode.setVisibility(View.VISIBLE);
                break;
        }

        binding.btnStartGame.setOnClickListener(v -> {
            viewModel.startGame(gameId);
        });

        binding.btnShareCode.setOnClickListener(v -> {
            // Share logic
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my PUT0 game! Code: " + gameId);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });

        // Setup RecyclerView for players
        binding.rvLobbyPlayers.setLayoutManager(new LinearLayoutManager(this));
        // Need an adapter for players list
    }

    private void setupObservers() {
        viewModel.getGameState().observe(this, state -> {
            if (state == null)
                return;

            binding.tvPlayerCount.setText("Players: " + state.getPlayers().size());

            // Check auto-start or enable start based on logic
            if (currentMode == MatchMode.SOLO_VS_AMIGO && state.getPlayers().size() >= 2) {
                binding.btnStartGame.setEnabled(true);
                binding.btnStartGame.setText("START GAME");
            }

            // Navigation to Game
            if (state.getStatus() == GameStatus.PLAYING) {
                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("GAME_ID", gameId);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                GameMessageHelper.showMessage(binding.getRoot(), error, GameMessageHelper.MessageType.NEUTRAL);
            }
        });
    }
}
