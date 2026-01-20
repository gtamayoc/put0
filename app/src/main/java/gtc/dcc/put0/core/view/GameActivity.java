package gtc.dcc.put0.core.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.adapter.CardAdapter;
import gtc.dcc.put0.core.adapter.PlayerListAdapter;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.data.model.GameStatus;
import gtc.dcc.put0.core.data.model.Player;
import gtc.dcc.put0.core.viewmodel.GameViewModel;
import gtc.dcc.put0.databinding.ActivityGameBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import gtc.dcc.put0.core.utils.CoreLogger;

public class GameActivity extends AppCompatActivity {
    private GameViewModel viewModel;
    private ActivityGameBinding binding;
    private CardAdapter playerHandAdapter;
    private CardAdapter playerVisibleAdapter;
    private CardAdapter playerHiddenAdapter;
    private CardAdapter tableCardsAdapter;
    private CardAdapter discardedCardsAdapter;
    private PlayerListAdapter playerAdapter;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadCompatibility();
        initializeViewModels();
        setupAdapters();
        setupUI();
        setupObservers();
        setupClickListeners();

        startChronometer();
    }

    private void loadCompatibility() {
        EdgeToEdge.enable(this);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
    }

    private void setupAdapters() {
        playerAdapter = new PlayerListAdapter(this, new ArrayList<>());

        playerHandAdapter = new CardAdapter(new ArrayList<>(), true, false);
        playerHandAdapter.setOnCardClickListener(new CardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(Card card, boolean isCurrentlyHidden) {
                if (!isCurrentlyHidden) {
                    // Play logic handled by button for now
                }
            }

            @Override
            public void onSelectionChanged(List<Card> selectedCards) {
                binding.btnPlaySelected.setEnabled(!selectedCards.isEmpty());
            }
        });

        playerVisibleAdapter = new CardAdapter(new ArrayList<>(), false, false);
        playerHiddenAdapter = new CardAdapter(new ArrayList<>(), false, true); // isHidden=true

        tableCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);
        discardedCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);

        playerHandAdapter.setMultipleSelectionEnabled(true);
        playerHandAdapter.setMaxSelectableCards(1); // Server usually takes 1 card at a time? Let's limit for safety or
                                                    // allow multi if server supports loop

        playerAdapter.setOnPlayerClickListener(new PlayerListAdapter.OnPlayerClickListener() {

            @Override
            public void onPlayerClick(Player player) {
                Toast.makeText(GameActivity.this, "Player: " + player.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSelectionChanged(List<Player> selectedPlayers) {
            }

        });
    }

    private void setupUI() {
        binding.tvGameState.setText("Connecting...");
        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Opponents are now flat in the layout or handled differently
        // If we want to keep playerAdapter, we need a container.
        // For now, let's just bypass userRecyclerView if it's gone.

        LinearLayoutManager playerHandManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvPlayerHand.setLayoutManager(playerHandManager);
        binding.rvPlayerHand.setAdapter(playerHandAdapter);

        // Visible Cards
        LinearLayoutManager playerVisibleManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvPlayerVisible.setLayoutManager(playerVisibleManager);
        binding.rvPlayerVisible.setAdapter(playerVisibleAdapter);

        // Hidden Cards
        LinearLayoutManager playerHiddenManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvPlayerHidden.setLayoutManager(playerHiddenManager);
        binding.rvPlayerHidden.setAdapter(playerHiddenAdapter);

        // Attach Swipe Helper for Player Hand
        gtc.dcc.put0.core.view.utils.CardTouchHelper callback = new gtc.dcc.put0.core.view.utils.CardTouchHelper(
                playerHandAdapter, position -> {
                    Card card = playerHandAdapter.getCards().get(position);
                    viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), card);
                    Toast.makeText(this, "Card Thrown: " + card.toString(), Toast.LENGTH_SHORT).show();
                    // Optional: Optimistic update or wait for server push
                });
        androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                callback);
        touchHelper.attachToRecyclerView(binding.rvPlayerHand);

        LinearLayoutManager tableCardsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.tableCards.setLayoutManager(tableCardsManager);
        binding.tableCards.setAdapter(tableCardsAdapter);

        LinearLayoutManager descartedCardsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false);
        binding.descartedCards.setLayoutManager(descartedCardsManager);
        binding.descartedCards.setAdapter(discardedCardsAdapter);
    }

    private void setupClickListeners() {
        binding.btnSortHand.setOnClickListener(v -> {
            playerHandAdapter.shuffleCards(); // Or sort
            Toast.makeText(this, "Sorted", Toast.LENGTH_SHORT).show();
        });

        binding.btnPlaySelected.setOnClickListener(v -> {
            List<Card> selectedCards = playerHandAdapter.getSelectedCards();
            if (!selectedCards.isEmpty()) {
                // Play each selected card (or just the first if server limit)
                for (Card c : selectedCards) {
                    viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), c);
                }
                playerHandAdapter.clearSelection();
            }
        });

        binding.btnSkipTurn.setOnClickListener(v -> {
            viewModel.drawCard(viewModel.getCurrentPlayerId().getValue());
            Toast.makeText(this, "Drawing Card", Toast.LENGTH_SHORT).show();
        });

        // btnGameInfo and btnOptions are replaced by settings icon
        binding.btnSettings.setOnClickListener(v -> showOptionsDialog());
    }

    private void setupObservers() {
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getGameState().observe(this, state -> {
            if (state == null)
                return;

            // Updated UI based on GameState
            binding.tvGameState.setText("Status: " + state.getStatus());

            // My Hand
            String myId = viewModel.getCurrentPlayerId().getValue();

            // Players (Top Section)
            updateOpponentsUI(state.getPlayers(), myId);

            // Table Cards
            tableCardsAdapter.updateData(state.getTable(), false);

            if (myId != null) {
                for (Player p : state.getPlayers()) {
                    if (p.getId().equals(myId)) {
                        playerHandAdapter.updateData(p.getHand(), true);
                        playerVisibleAdapter.updateData(p.getVisibleCards(), false);
                        playerHiddenAdapter.updateData(p.getHiddenCards(), false);

                        binding.tvPlayerName.setText(p.getName());
                        // binding.tvPlayerScore.setText(String.valueOf(p.getScore())); // If score
                        // exists
                        break;
                    }
                }
            }
        });

        viewModel.getGameEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    switch (event) {
                        case CLEAN_TABLE:
                            playAnimation(R.raw.clean_table);
                            break;
                        case GAME_WON:
                            playAnimation(R.raw.winner);
                            break;
                        // Add other events as needed
                    }
                });
    }

    private void updateOpponentsUI(List<Player> players, String myId) {
        List<Player> opponents = new ArrayList<>();
        for (Player p : players) {
            if (!p.getId().equals(myId)) {
                opponents.add(p);
            }
        }

        // Slot 1
        if (opponents.size() > 0) {
            binding.opponent1.setVisibility(android.view.View.VISIBLE);
            binding.tvOpponentName1.setText(opponents.get(0).getName());
            binding.tvOpponentCards1.setText(opponents.get(0).getHand().size() + " cards");
            binding.opponent1.setAlpha(
                    opponents.get(0).getId().equals(viewModel.getGameState().getValue().getCurrentPlayerId()) ? 1.0f
                            : 0.7f);
        } else {
            binding.opponent1.setVisibility(android.view.View.GONE);
        }

        // Slot 2
        if (opponents.size() > 1) {
            binding.opponent2.setVisibility(android.view.View.VISIBLE);
            binding.tvOpponentName2.setText(opponents.get(1).getName());
            binding.tvOpponentStatus2.setText(opponents.get(1).getHand().size() + " cards");
            binding.opponent2.setAlpha(
                    opponents.get(1).getId().equals(viewModel.getGameState().getValue().getCurrentPlayerId()) ? 1.0f
                            : 0.7f);
        } else {
            binding.opponent2.setVisibility(android.view.View.GONE);
        }

        // Slot 3
        if (opponents.size() > 2) {
            binding.opponent3.setVisibility(android.view.View.VISIBLE);
            binding.tvOpponentName3.setText(opponents.get(2).getName());
            binding.tvOpponentCards3.setText(opponents.get(2).getHand().size() + " cards");
            binding.opponent3.setAlpha(
                    opponents.get(2).getId().equals(viewModel.getGameState().getValue().getCurrentPlayerId()) ? 1.0f
                            : 0.7f);
        } else {
            binding.opponent3.setVisibility(android.view.View.GONE);
        }
    }

    private void playAnimation(int rawResId) {
        binding.lottieAnimationView.setAnimation(rawResId);
        binding.lottieAnimationView.setVisibility(android.view.View.VISIBLE);
        binding.lottieAnimationView.playAnimation();
        binding.lottieAnimationView.addAnimatorListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                binding.lottieAnimationView.setVisibility(android.view.View.GONE);
            }
        });
    }

    private void startChronometer() {
        chronometer = binding.gameTimer;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void showGameInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Game Info")
                .setMessage(String.format("Time: %s\nPlayers: %d",
                        chronometer.getText(),
                        playerAdapter.getItemCount()))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showOptionsDialog() {
        String[] options = { "Leave Game", "Settings" };
        new AlertDialog.Builder(this)
                .setTitle("Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0)
                        showExitConfirmationDialog();
                })
                .show();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    viewModel.leaveGame();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chronometer != null) {
            chronometer.stop();
        }
    }
}