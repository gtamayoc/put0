package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

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

        initializeViewModel();
        handleIntentData();
        setupUI();
        setupObservers();
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
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
