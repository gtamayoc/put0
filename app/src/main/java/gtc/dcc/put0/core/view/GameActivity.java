package gtc.dcc.put0.core.view;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

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
    private androidx.recyclerview.widget.ItemTouchHelper touchHelper;
    private int lastHandSize = -1; // Track hand size to detect failed swipes

    // Phase tracking for detecting transitions and failures
    private int previousHandSize = -1;
    private int previousHiddenSize = -1;
    private boolean wasInPhase4 = false;

    // Game status tracking
    private boolean gameOverShown = false;
    private List<String> matchHistory = new ArrayList<>();

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

        // Setup swipe functionality
        setupSwipeHelper();

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

    private void setupSwipeHelper() {
        // Detach old helper if exists
        if (touchHelper != null) {
            touchHelper.attachToRecyclerView(null);
        }

        // Create new callback and helper
        gtc.dcc.put0.core.view.utils.CardTouchHelper callback = new gtc.dcc.put0.core.view.utils.CardTouchHelper(
                playerHandAdapter, (position, card) -> {
                    // Card is passed directly from the swipe helper
                    // No need to get it from adapter since it was already removed
                    viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), card);
                });
        touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rvPlayerHand);
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

        // Profile/Settings click area
        binding.layoutProfileTrigger.setOnClickListener(v -> showOptionsDialog());
        binding.btnHistory.setOnClickListener(v -> showHistoryDialog());

        binding.discardPileContainer.setOnClickListener(v -> showDiscardedCardsDialog());
    }

    private void setupObservers() {
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                // Map server errors to user-friendly messages
                String friendlyMessage = mapErrorToFriendlyMessage(error);
                GameMessageHelper.showMessage(binding.getRoot(), friendlyMessage,
                        GameMessageHelper.MessageType.NEUTRAL);

                // CRITICAL FIX: Recreate ItemTouchHelper to fully reset swipe functionality
                binding.rvPlayerHand.post(() -> {
                    setupSwipeHelper();
                    playerHandAdapter.notifyDataSetChanged();
                });
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
            boolean deckNotEmpty = state.getMainDeck() != null && !state.getMainDeck().isEmpty();
            if (state.getMainDeck() != null) {
                binding.tvInitialDeck.setText(String.valueOf(state.getMainDeck().size()));
                // Visual feedback if deck is empty (F2 trigger)
                binding.tvInitialDeck.setAlpha(state.getMainDeck().isEmpty() ? 0.3f : 1.0f);

                // Update Deck Type Label
                int deckSize = state.getDeckSize();
                String typeStr = (deckSize <= 52) ? "SIMPLE (52)" : "DOBLE (104)";
                binding.tvDeckType.setText(typeStr);
                binding.tvDeckType.setAlpha(state.getMainDeck().isEmpty() ? 0.3f : 0.8f);
            }

            // 2. Update Table Pile (Active Cards)
            tableCardsAdapter.updateData(state.getTablePile(), false);
            if (state.getTablePile() != null && !state.getTablePile().isEmpty()) {
                binding.tableCards.smoothScrollToPosition(state.getTablePile().size() - 1);
            }

            // 3. Update Discard Pile (Burned Cards)
            if (state.getDiscardPile() != null) {
                binding.tvDiscardCount.setText(String.valueOf(state.getDiscardPile().size()));
                discardedCardsAdapter.updateData(state.getDiscardPile(), false);
            }

            // 4. Update Game Status / Phase Display
            updateGamePhaseDisplay(state);

            // 5. Update Match History
            if (state.getLastAction() != null && !state.getLastAction().isEmpty()) {
                String latest = state.getLastAction();
                if (matchHistory.isEmpty() || !matchHistory.get(0).equals(latest)) {
                    matchHistory.add(0, latest);
                    if (matchHistory.size() > 50)
                        matchHistory.remove(50);
                }
            }

            // 6. Check for Game Over
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

                            // Phase 4 detection: when player has no hand/visible but has hidden cards
                            // OR if they somehow have hidden cards in hand (legacy support)
                            boolean isPhase4 = false;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                isPhase4 = (p.getHand().isEmpty() && p.getVisibleCards().isEmpty()
                                        && !p.getHiddenCards().isEmpty())
                                        || p.getHand().stream().anyMatch(Card::isHidden);
                            }

                            // Update Hand with standard DiffUtil animations
                            // We pass deckNotEmpty to maintain a 3-slot layout if cards are available
                            playerHandAdapter.updateData(p.getHand(), !isPhase4, deckNotEmpty);

                            // If swipe failed (detected by hand size), recreate the ItemTouchHelper
                            if (lastHandSize != -1 && p.getHand().size() >= lastHandSize) {
                                binding.rvPlayerHand.post(() -> setupSwipeHelper());
                            }

                            lastHandSize = p.getHand().size();
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
        List<Card> hidden = player.getHiddenCards();
        int hiddenCount = hidden != null ? hidden.size() : 0;

        // Detect Phase 4 status for interaction
        int currentHandSize = player.getHand() != null ? player.getHand().size() : 0;
        int currentVisibleSize = player.getVisibleCards() != null ? player.getVisibleCards().size() : 0;
        boolean canPlayFromHidden = currentHandSize == 0 && currentVisibleSize == 0 && hiddenCount > 0;

        // Image Click Listeners
        for (int i = 0; i < 3; i++) {
            final int index = i;
            ImageView iv = (i == 0) ? binding.ivHidden1 : (i == 1) ? binding.ivHidden2 : binding.ivHidden3;

            if (hiddenCount > i) {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(R.drawable.base); // ALWAYS show back for hidden cards
                iv.setOnClickListener(v -> {
                    if (canPlayFromHidden) {
                        // Play the hidden card directly
                        Card cardToPlay = hidden.get(index);
                        viewModel.playCard(viewModel.getCurrentPlayerId().getValue(), cardToPlay);
                    } else {
                        GameMessageHelper.showMessage(binding.getRoot(), R.string.msg_card_not_available_alt,
                                GameMessageHelper.MessageType.INFO);
                    }
                });
            } else {
                iv.setVisibility(View.GONE);
            }
        }

        CoreLogger.d("GAME_UI",
                "Static Hidden Cards updated: " + hiddenCount + " (Can play: " + canPlayFromHidden + ")");

        // Update Visible Cards (Static Images)
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

        iv.setOnClickListener(v -> {
            GameMessageHelper.showMessage(binding.getRoot(), R.string.msg_card_not_available_alt, // "Juega primero las
                                                                                                  // cartas de tu mano"
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

        List<Card> discarded = new ArrayList<>(viewModel.getGameState().getValue().getDiscardPile());
        // Reverse so the most recently discarded cards appear first
        java.util.Collections.reverse(discarded);

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
        boolean isInPhase4 = deckEmpty && currentVisibleSize == 0 && currentHiddenSize > 0 && currentHandSize == 0;

        // Detect transition INTO Phase 4 (Now based on hidden pile accessibility)
        if (!wasInPhase4 && isInPhase4) {
            CoreLogger.i("PHASE_4", "Entered Phase 4 - Hidden Cards Play Mode");
            GameMessageHelper.showMessage(
                    binding.getRoot(),
                    R.string.msg_phase_hidden,
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

        // Detect BLIND PLAY FAILURE (In new system: hand size goes from 0 to > 0)
        if (wasInPhase4 && previousHandSize == 0 && currentHandSize > 0) {
            CoreLogger.i("PHASE_4", "Blind play failed: collected " + currentHandSize + " cards");

            // Show failure message
            GameMessageHelper.showMessage(
                    binding.getRoot(),
                    R.string.msg_blind_failed, // "No fue posible"
                    GameMessageHelper.MessageType.NEUTRAL,
                    GameMessageHelper.DURATION_SHORT);

            wasInPhase4 = false; // Important: Exit phase 4 on failure
        }

        // Update tracking variables
        previousHandSize = currentHandSize;
        previousHiddenSize = currentHiddenSize;

        // Exit Phase 4 if hand is no longer empty (e.g. collected table)
        // OR if hidden cards are gone
        if (wasInPhase4 && (currentHandSize > 0 || currentHiddenSize == 0)) {
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
    private void showHistoryDialog() {
        if (matchHistory.isEmpty()) {
            GameMessageHelper.showMessage(binding.getRoot(), "Aún no hay jugadas en esta ronda",
                    GameMessageHelper.MessageType.INFO);
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvHistory);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseHistory);

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(
                            @NonNull android.view.ViewGroup parent, int viewType) {
                        View v = getLayoutInflater().inflate(R.layout.item_history, parent, false);
                        return new RecyclerView.ViewHolder(v) {
                        };
                    }

                    @Override
                    public void onBindViewHolder(
                            @NonNull RecyclerView.ViewHolder holder,
                            int position) {
                        String action = matchHistory.get(position);
                        TextView tvText = holder.itemView.findViewById(R.id.tvActionText);
                        TextView tvTime = holder.itemView.findViewById(R.id.tvTimeText);
                        View indicator = holder.itemView.findViewById(R.id.indicator);

                        tvText.setText(action);

                        // Visual variation for latest move
                        if (position == 0) {
                            tvTime.setText("Última acción");
                            tvTime.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.md_primary));
                            indicator.setAlpha(1.0f);
                        } else {
                            tvTime.setText("Anterior");
                            tvTime.setTextColor(0x88FFFFFF);
                            indicator.setAlpha(0.3f);
                        }
                    }

                    @Override
                    public int getItemCount() {
                        return matchHistory.size();
                    }
                });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        rv.scrollToPosition(0);
    }

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

        GameState state = viewModel.getGameState().getValue();

        // 1. Must be my turn
        String myId = viewModel.getCurrentPlayerId().getValue();
        if (state.getCurrentPlayer() == null || !state.getCurrentPlayer().getId().equals(myId)) {
            return false;
        }

        // 2. If it's a hidden card in hand (Phase 4), it's always "tryable"
        if (card.isHidden()) {
            return true;
        }

        // 3. Normal Power Rule
        Card topCard = state.getTopCard();
        if (topCard == null)
            return true;

        // Special cards (2 and 10) are always playable
        if (card.getRankValue() == 2 || card.getRankValue() == 10) {
            return true;
        }

        // If top card is 2, any card is playable
        if (topCard.getRankValue() == 2) {
            return true;
        }

        // Otherwise compare power (Ace is highest)
        int cardPower = card.getRankValue() == 1 ? 14 : card.getRankValue();
        int topPower = topCard.getRankValue() == 1 ? 14 : topCard.getRankValue();

        return cardPower >= topPower;
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