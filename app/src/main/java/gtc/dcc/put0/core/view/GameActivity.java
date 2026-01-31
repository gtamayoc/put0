package gtc.dcc.put0.core.view;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import androidx.core.content.ContextCompat;

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
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.viewmodel.GameViewModel;
import gtc.dcc.put0.databinding.ActivityGameBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import gtc.dcc.put0.core.utils.CoreLogger;
import gtc.dcc.put0.core.utils.GameMessageHelper;

public class GameActivity extends AppCompatActivity {
    private GameViewModel viewModel;
    private ActivityGameBinding binding;
    private CardAdapter playerHandAdapter;
    private CardAdapter tableCardsAdapter;
    private CardAdapter discardedCardsAdapter;
    private PlayerListAdapter playerAdapter;
    private Chronometer chronometer;

    // Phase tracking for detecting transitions and failures
    private int previousHandSize = -1;
    private int previousHiddenSize = -1;
    private boolean wasInPhase4 = false;

    // Game over tracking
    private boolean gameOverShown = false;

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
                if (selectedCards.isEmpty()) {
                    binding.btnPlaySelected.setEnabled(false);
                    // Dim the button slightly or change color to indicate disabled state
                    // effectively
                    binding.btnPlaySelected.setAlpha(0.5f);
                } else {
                    Card selected = selectedCards.get(0);
                    boolean playable = isCardPlayable(selected);
                    binding.btnPlaySelected.setEnabled(playable);
                    binding.btnPlaySelected.setAlpha(playable ? 1.0f : 0.5f);
                }
            }
        });

        tableCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);
        discardedCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);

        playerHandAdapter.setMultipleSelectionEnabled(true);
        playerHandAdapter.setMaxSelectableCards(1);

        playerAdapter.setOnPlayerClickListener(new PlayerListAdapter.OnPlayerClickListener() {
            @Override
            public void onPlayerClick(Player player) {
                // Debug message removed - not needed for production
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

        // Attach Swipe Helper for Player Hand
        gtc.dcc.put0.core.view.utils.CardTouchHelper callback = new gtc.dcc.put0.core.view.utils.CardTouchHelper(
                playerHandAdapter, position -> {
                    Card card = playerHandAdapter.getCards().get(position);
                    viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), card);
                    // Visual feedback handled by animation, no text message needed
                });
        androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                callback);
        touchHelper.attachToRecyclerView(binding.rvPlayerHand);

        // Add overlap decoration (-20% of assuming 80dp width = -16dp, roughly 40px)
        int overlapPx = (int) (getResources().getDisplayMetrics().density * 30); // 30dp overlap
        binding.rvPlayerHand.addItemDecoration(new gtc.dcc.put0.core.view.utils.HorizontalOverlapDecoration(overlapPx));

        LinearLayoutManager tableCardsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.tableCards.setLayoutManager(tableCardsManager);
        binding.tableCards.addItemDecoration(new gtc.dcc.put0.core.view.utils.HorizontalOverlapDecoration(overlapPx)); // Overlap
                                                                                                                       // for
                                                                                                                       // table
        binding.tableCards.setAdapter(tableCardsAdapter);

    }

    private void setupClickListeners() {

        binding.btnPlaySelected.setOnClickListener(v -> {
            List<Card> selectedCards = playerHandAdapter.getSelectedCards();
            if (!selectedCards.isEmpty()) {
                String userName = SharedPreferenceManager.getString("user_name", "Player");
                // Play each selected card (or just the first if server limit)
                for (Card c : selectedCards) {
                    viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), c);
                }
                playerHandAdapter.clearSelection();
            }
        });

        binding.btnSkipTurn.setOnClickListener(v -> {
            viewModel.collectTable(viewModel.getCurrentPlayerId().getValue());
            GameMessageHelper.showMessage(binding.getRoot(), R.string.msg_cards_collected,
                    GameMessageHelper.MessageType.NEUTRAL);
        });

        // btnGameInfo and btnOptions are replaced by settings icon
        // btnGameInfo and btnOptions are replaced by settings icon
        binding.btnSettings.setOnClickListener(v -> showOptionsDialog());
        binding.btnHistory.setOnClickListener(v -> {
            // TODO: Implement history dialog
        });

        binding.discardPileContainer.setOnClickListener(v -> showDiscardedCardsDialog());
    }

    private void setupObservers() {
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                // Map server errors to user-friendly messages
                String friendlyMessage = mapErrorToFriendlyMessage(error);
                GameMessageHelper.showMessage(binding.getRoot(), friendlyMessage,
                        GameMessageHelper.MessageType.NEUTRAL);

                // CRITICAL FIX: Refresh adapter to restore any cards that were swiped away
                // locally but rejected by the server/logic.
                playerHandAdapter.notifyDataSetChanged();
            }
        });

        viewModel.getGameState().observe(this, state -> {
            if (state == null)
                return;

            // My Hand (Current Player)
            String myId = viewModel.getCurrentPlayerId().getValue();

            // Players (Top Section)
            updateOpponentsUI(state.getPlayers(), myId);

            // 1. Update Main Deck Counter
            if (state.getMainDeck() != null) {
                binding.tvInitialDeck.setText(String.valueOf(state.getMainDeck().size()));
                // Visual feedback if deck is empty (F2 trigger)
                binding.tvInitialDeck.setAlpha(state.getMainDeck().isEmpty() ? 0.3f : 1.0f);
            }

            // 2. Update Table Pile (Active Cards)
            tableCardsAdapter.updateData(state.getTablePile(), false);

            // 3. Update Discard Pile (Burned Cards)
            if (state.getDiscardPile() != null) {
                binding.tvDiscardCount.setText(String.valueOf(state.getDiscardPile().size()));
                discardedCardsAdapter.updateData(state.getDiscardPile(), false);
            }

            // 4. Update Game Status / Phase Display
            updateGamePhaseDisplay(state);

            // 5. Check for Game Over
            checkGameOver(state);

            if (myId != null) {
                // Show last action if available (Local or Server)
                // if (state.getLastAction() != null && !state.getLastAction().isEmpty()) {
                // //GameMessageHelper.showMessage(binding.getRoot(), state.getLastAction(),
                // GameMessageHelper.MessageType.INFO);
                // }

                for (Player p : state.getPlayers()) {
                    if (p.getId().equals(myId)) {
                        // Detect Phase 4 transitions and failures
                        detectPhase4Events(p, state);

                        // Active Hand (RecyclerView)
                        if (p.getHand() != null) {
                            CoreLogger.d("GAME_STATE", "Active Hand Cards: " + p.getHand().size());
                            boolean isPhase4 = false;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                isPhase4 = p.getHand().stream().anyMatch(Card::isHidden);
                            }
                            playerHandAdapter.updateData(p.getHand(), !isPhase4); // Only sort if NOT in Phase 4
                        }

                        // Static Piles (Hidden & Visible)
                        updateStaticPiles(p);

                        // Ensure name is up to date from preferences if available
                        String prefName = SharedPreferenceManager.getString("user_name", null);
                        if (prefName != null && !prefName.isEmpty() && p.getName().equals("Player")) {
                            binding.tvPlayerName.setText(prefName);
                        } else {
                            binding.tvPlayerName.setText(p.getName());
                        }
                        break;
                    }
                }
            }
        });

        viewModel.getGameEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    CoreLogger.d("GAME_EVENT", "Received event: " + event);
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

    private void updateStaticPiles(Player player) {
        // Update Hidden Cards (Static Images)
        // Hidden cards are just "markers" (face down) until Phase 4?
        // Or if they are in Phase 4, they might be blindly clickable.
        // For now, simple visualization of presence.
        List<Card> hidden = player.getHiddenCards();
        int hiddenCount = hidden != null ? hidden.size() : 0;

        binding.ivHidden1.setVisibility(hiddenCount >= 1 ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.ivHidden2.setVisibility(hiddenCount >= 2 ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.ivHidden3.setVisibility(hiddenCount >= 3 ? android.view.View.VISIBLE : android.view.View.GONE);

        CoreLogger.d("GAME_UI", "Static Hidden Cards updated: " + hiddenCount);

        // Update Visible Cards (Static Images)
        // These show the actual card face but are static until Phase 3 active.
        List<Card> visible = player.getVisibleCards();
        int visibleCount = visible != null ? visible.size() : 0;

        updateStaticCardView(binding.ivVisible1, visibleCount >= 1 ? visible.get(0) : null);
        updateStaticCardView(binding.ivVisible2, visibleCount >= 2 ? visible.get(1) : null);
        updateStaticCardView(binding.ivVisible3, visibleCount >= 3 ? visible.get(2) : null);

        CoreLogger.d("GAME_UI", "Static Visible Cards updated: " + visibleCount);
    }

    private void updateStaticCardView(android.widget.ImageView iv, Card card) {
        if (card == null) {
            iv.setVisibility(android.view.View.GONE);
            return;
        }
        iv.setVisibility(android.view.View.VISIBLE);
        int resId = gtc.dcc.put0.core.utils.DeckUtils.getCardResourceId(this, card);
        iv.setImageResource(resId);

        // Optional: Make strictly interactive only if Phase allows?
        // For now, static representation as requested.
        iv.setOnClickListener(v -> {
            GameMessageHelper.showMessage(binding.getRoot(), R.string.msg_card_not_available,
                    GameMessageHelper.MessageType.INFO);
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
        binding.lottieAnimationView.setRepeatCount(0); // Ensure no loop
        binding.lottieAnimationView.setVisibility(android.view.View.VISIBLE);
        binding.lottieAnimationView.playAnimation();
        binding.lottieAnimationView.addAnimatorListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                binding.lottieAnimationView.setVisibility(android.view.View.GONE);
                binding.lottieAnimationView.removeAllAnimatorListeners();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                binding.lottieAnimationView.setVisibility(android.view.View.GONE);
                binding.lottieAnimationView.removeAllAnimatorListeners();
            }
        });
    }

    private void startChronometer() {
        chronometer = binding.gameTimer;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void updateGamePhaseDisplay(GameState state) {
        // 1. Determine Phase
        boolean deckEmpty = state.getMainDeck() == null || state.getMainDeck().isEmpty();
        String phaseName = "F1: Robando";
        int phaseIndex = 1;

        if (deckEmpty) {
            phaseName = "F2: Mano";
            phaseIndex = 2;
        }

        Player currentPlayer = state.getCurrentPlayer();
        if (currentPlayer != null && deckEmpty) {
            if (currentPlayer.getHand().isEmpty() && !currentPlayer.getVisibleCards().isEmpty()) {
                phaseName = "F3: Visibles";
                phaseIndex = 3;
            } else if (currentPlayer.getHand().isEmpty() && currentPlayer.getVisibleCards().isEmpty()
                    && !currentPlayer.getHiddenCards().isEmpty()) {
                phaseName = "F4: Ocultas";
                phaseIndex = 4;
            }
        }

        // 2. Update Status Panel
        binding.tvGameState.setText(phaseName);

        // Update Dots
        int activeColor = ContextCompat.getColor(this, R.color.md_primary);
        int inactiveColor = 0x33FFFFFF;
        binding.phaseDot1.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(phaseIndex >= 1 ? activeColor : inactiveColor));
        binding.phaseDot2.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(phaseIndex >= 2 ? activeColor : inactiveColor));
        binding.phaseDot3.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(phaseIndex >= 3 ? activeColor : inactiveColor));
        binding.phaseDot4.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(phaseIndex >= 4 ? activeColor : inactiveColor));

        // Update Last Action
        if (state.getLastAction() != null && !state.getLastAction().isEmpty()) {
            binding.tvLastAction.setText(state.getLastAction());
        }

        // Update Table Target
        Card topCard = state.getTopCard();
        if (topCard != null) {
            String target = "Superar " + gtc.dcc.put0.core.utils.DeckUtils.getCardShortDescription(topCard);
            binding.tvTableTarget.setText(target);
            // Color based on suit (red for hearts/diamonds, else white)
            boolean isRed = topCard.getSuit() == gtc.dcc.put0.core.model.Suit.HEARTS
                    || topCard.getSuit() == gtc.dcc.put0.core.model.Suit.DIAMONDS;
            binding.tvTableTarget.setTextColor(isRed ? 0xFFFF5252 : 0xFFFFFFFF);
        } else {
            binding.tvTableTarget.setText("Cualquier carta");
            binding.tvTableTarget.setTextColor(0xFFFFFFFF);
        }

        // 3. Update Turn Indicator & Buttons State
        if (state.getCurrentPlayer() != null) {
            String turnText = "Turno: " + state.getCurrentPlayer().getName();
            binding.tvTurnIndicator.setText(turnText);

            String myId = viewModel.getCurrentPlayerId().getValue();
            boolean isMyTurn = state.getCurrentPlayer().getId().equals(myId);
            binding.tvTurnIndicator.setAlpha(isMyTurn ? 1.0f : 0.7f);
            binding.tvTurnIndicator.setBackgroundResource(isMyTurn ? R.drawable.rounded_edittext : 0);

            boolean canCollect = isMyTurn && state.getTablePile() != null && !state.getTablePile().isEmpty();
            binding.btnSkipTurn.setEnabled(canCollect);
            binding.btnSkipTurn.setAlpha(canCollect ? 1.0f : 0.5f);

            // Highlight panel if it's my turn
            binding.panelGameStatus.setAlpha(isMyTurn ? 1.0f : 0.85f);
        }
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

    private void showDiscardedCardsDialog() {
        if (viewModel.getGameState().getValue() == null || viewModel.getGameState().getValue().getDiscardPile() == null)
            return;

        List<Card> discarded = viewModel.getGameState().getValue().getDiscardPile();

        // Use a simple list dialog or custom view
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(this);
        rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        CardAdapter dialogAdapter = new CardAdapter(discarded, false, false);
        rv.setAdapter(dialogAdapter);
        rv.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Discarded Cards (" + discarded.size() + ")")
                .setView(rv)
                .setPositiveButton("Close", null)
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

    /**
     * Detects Phase 4 events: transitions, blind play failures, and regressions.
     * Shows appropriate messages based on game state changes.
     */
    private void detectPhase4Events(Player currentPlayer, GameState state) {
        int currentHandSize = currentPlayer.getHand() != null ? currentPlayer.getHand().size() : 0;
        int currentHiddenSize = currentPlayer.getHiddenCards() != null ? currentPlayer.getHiddenCards().size() : 0;
        int currentVisibleSize = currentPlayer.getVisibleCards() != null ? currentPlayer.getVisibleCards().size() : 0;

        boolean deckEmpty = state.getMainDeck() == null || state.getMainDeck().isEmpty();
        boolean isInPhase4 = deckEmpty && currentVisibleSize == 0 && currentHiddenSize > 0 && currentHandSize > 0;

        // Check if any cards in hand are marked as hidden (blind play)
        boolean hasHiddenCardsInHand = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            hasHiddenCardsInHand = currentPlayer.getHand() != null &&
                    currentPlayer.getHand().stream().anyMatch(Card::isHidden);
        }

        // Detect transition INTO Phase 4
        if (!wasInPhase4 && isInPhase4 && hasHiddenCardsInHand) {
            CoreLogger.i("PHASE_4", "Entered Phase 4 - Blind Play Mode");
            GameMessageHelper.showMessage(
                    binding.getRoot(),
                    R.string.msg_phase_hidden, // "Jugando a ciegas"
                    GameMessageHelper.MessageType.PHASE,
                    GameMessageHelper.DURATION_MEDIUM);
            wasInPhase4 = true;
        }

        // Detect REGRESSION from Phase 4 (hidden cards moved back to hidden pile)
        if (wasInPhase4 && previousHiddenSize != -1 && currentHiddenSize > previousHiddenSize) {
            int cardsMovedBack = currentHiddenSize - previousHiddenSize;
            CoreLogger.i("PHASE_4", "Phase 4 Regression detected: " + cardsMovedBack + " cards moved back");

            // Show regression message
            GameMessageHelper.showMessage(
                    binding.getRoot(),
                    R.string.msg_phase_regression, // "Las cartas vuelven a la pila"
                    GameMessageHelper.MessageType.PHASE,
                    GameMessageHelper.DURATION_MEDIUM);

            // Show "still playing" encouragement
            binding.getRoot().postDelayed(() -> {
                GameMessageHelper.showMessage(
                        binding.getRoot(),
                        R.string.msg_still_playing, // "Sigues en juego"
                        GameMessageHelper.MessageType.INFO,
                        GameMessageHelper.DURATION_SHORT);
            }, 2500); // Show after regression message

            wasInPhase4 = false; // Exited Phase 4
        }

        // Detect BLIND PLAY FAILURE (hand size increased or stayed same = collected
        // table)
        // If hand size didn't decrease after playing a card, it means player picked up
        // table
        if (wasInPhase4 && previousHandSize != -1 && currentHandSize >= previousHandSize && !currentPlayer.hasWon()) {
            int cardsCollected = currentHandSize - (previousHandSize - 1); // Approx
            CoreLogger.i("PHASE_4", "Blind play failed: collected " + cardsCollected + " cards");

            // Show failure message
            GameMessageHelper.showMessage(
                    binding.getRoot(),
                    R.string.msg_blind_failed, // "No fue posible"
                    GameMessageHelper.MessageType.NEUTRAL,
                    GameMessageHelper.DURATION_SHORT);
        }

        // Update tracking variables
        previousHandSize = currentHandSize;
        previousHiddenSize = currentHiddenSize;

        // Exit Phase 4 if no more hidden cards in hand
        if (wasInPhase4 && !hasHiddenCardsInHand) {
            wasInPhase4 = false;
        }
    }

    /**
     * Checks if the game is over and shows victory/defeat dialog.
     */
    private void checkGameOver(GameState state) {
        if (gameOverShown) {
            return; // Already shown, don't show again
        }

        if (state.getStatus() == GameStatus.FINISHED && state.getWinnerId() != null) {
            gameOverShown = true;

            String myId = viewModel.getCurrentPlayerId().getValue();
            boolean iWon = state.getWinnerId().equals(myId);

            // Find winner name
            String winnerName = "Jugador";
            for (Player p : state.getPlayers()) {
                if (p.getId().equals(state.getWinnerId())) {
                    winnerName = p.getName();
                    break;
                }
            }

            showGameOverDialog(iWon, winnerName);
        }
    }

    /**
     * Shows the game over dialog with victory or defeat animation.
     */
    private void showGameOverDialog(boolean victory, String winnerName) {
        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_over, null);

        // Create dialog
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Make dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Get views
        com.airbnb.lottie.LottieAnimationView lottieView = dialogView.findViewById(R.id.lottieGameOver);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvGameOverTitle);
        android.widget.TextView tvWinnerName = dialogView.findViewById(R.id.tvWinnerName);
        com.google.android.material.button.MaterialButton btnNewGame = dialogView.findViewById(R.id.btnNewGame);
        com.google.android.material.button.MaterialButton btnBackToMenu = dialogView.findViewById(R.id.btnBackToMenu);

        // Configure based on victory or defeat
        if (victory) {
            lottieView.setAnimation(R.raw.winner);
            tvTitle.setText("¡Ganaste!");
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.msg_success));
        } else {
            lottieView.setAnimation(R.raw.clean_table); // Use clean_table as defeat animation
            tvTitle.setText("Fin de la Partida");
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.msg_neutral));
        }

        tvWinnerName.setText(winnerName);
        lottieView.playAnimation();

        // Button listeners
        btnNewGame.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.leaveGame(); // Essential to clear state
            finish(); // Go back to menu to start fresh
        });

        btnBackToMenu.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.leaveGame(); // Essential to clear state
            finish(); // Go back to MainActivity
        });

        dialog.show();
    }

    /**
     * Maps server error messages to user-friendly messages.
     * Avoids technical jargon and blaming the player.
     */
    /**
     * Checks if the selected card is valid to play against the current table.
     * Rules:
     * 1. If table is empty, any card is playable.
     * 2. If table has cards, played card must be higher rank of SAME SUIT.
     * 3. Special case: Ace (1) is usually highest or trump, need to check Rules.
     * For simplicty here: If table has card, must be same suit and higher value.
     */
    private boolean isCardPlayable(Card card) {
        if (viewModel.getGameState().getValue() == null)
            return false;

        List<Card> tablePile = viewModel.getGameState().getValue().getTablePile();
        if (tablePile == null || tablePile.isEmpty()) {
            return true; // Any card to start
        }

        Card topCard = tablePile.get(tablePile.size() - 1);

        // Logic: Must match suit and be higher value
        if (card.getSuit() == topCard.getSuit()) {
            // Check Ace special rule if needed, otherwise compare rankValue
            return card.getRankValue() > topCard.getRankValue();
        } else {
            // Taking table logic? Or just invalid?
            // Usually if different suit, it's a kill or discard...
            // For button enable, let's be strict: Same suit higher only, OR potentially a
            // trump if implemented.
            // For now, assume strict follow suit.
            return false;
        }
    }

    private String mapErrorToFriendlyMessage(String error) {
        if (error == null)
            return "Unknown error";
        if (error.contains("Network"))
            return "Error de red. Intenta de nuevo.";
        if (error.contains("not your turn"))
            return "No es tu turno aún.";
        if (error.contains("Invalid card"))
            return "Esta carta no se puede jugar ahora.";
        return error;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chronometer != null) {
            chronometer.stop();
        }
        // Clean up message helper resources
        GameMessageHelper.cleanup();
    }
}