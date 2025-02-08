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
import java.util.Collections;
import java.util.List;

import gtc.dcc.put0.core.adapter.CardAdapter;
import gtc.dcc.put0.core.adapter.PlayerAdapter;
import gtc.dcc.put0.core.adapter.PlayerListAdapter;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.GamePhase;
import gtc.dcc.put0.core.model.Player;
import gtc.dcc.put0.core.viewmodel.GameViewModel;
import gtc.dcc.put0.databinding.ActivityGameBinding;

public class GameActivity extends AppCompatActivity {
    private GameViewModel viewModel;
    private ActivityGameBinding binding;
    private CardAdapter playerHandAdapter;
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

        viewModel.initializeGame(2, this);
        startChronometer();
    }

    private void loadCompatibility() {
        EdgeToEdge.enable(this);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
    }


    private void setupAdapters() {
        // Configurar adaptador de jugadores
        playerAdapter = new PlayerListAdapter(this, new ArrayList<>());

        // Configurar adaptador de mano del jugador
        playerHandAdapter = new CardAdapter(new ArrayList<>(), true, false);
        playerHandAdapter.setOnCardClickListener(new CardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(Card card, boolean isCurrentlyHidden) {
                if (!isCurrentlyHidden) {
                    //viewModel.playCard(card);
                }
            }

            @Override
            public void onSelectionChanged(List<Card> selectedCards) {
                binding.btnPlaySelected.setEnabled(!selectedCards.isEmpty());
            }
        });

        // Configurar adaptador de cartas en mesa
        tableCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);

        // Configurar adaptador de cartas en mesa
        discardedCardsAdapter = new CardAdapter(new ArrayList<>(), false, false);

        // Habilitar selección múltiple para la mano del jugador
        playerHandAdapter.setMultipleSelectionEnabled(true);
        playerHandAdapter.setMaxSelectableCards(4);

        // Configurar el listener
        playerAdapter.setOnPlayerClickListener(new PlayerListAdapter.OnPlayerClickListener() {
            @Override
            public void onPlayerClick(Player player) {
                // Manejar clic en un jugador
                Toast.makeText(GameActivity.this, "Clic en: " + player.getNames(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSelectionChanged(List<Player> selectedPlayers) {
                // Manejar cambios en la selección
                if (selectedPlayers.isEmpty()) {
                    Toast.makeText(GameActivity.this, "No hay jugadores seleccionados", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GameActivity.this, "Jugadores seleccionados: " + selectedPlayers.size(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupUI() {
        // Configurar datos iniciales de UI
        binding.tvRound.setText("Ronda 1");
        binding.tvGameState.setText("Estado: En Progreso");
        binding.tvInitialDeck.setText("56/104");
        binding.tvTableCards.setText("Mesa: 0 cartas");

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerView de jugadores
        binding.userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.userRecyclerView.setAdapter(playerAdapter);

        // Configurar RecyclerView de mano del jugador
        LinearLayoutManager playerHandManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.playerHiddenCardsFixed.setLayoutManager(playerHandManager);
        binding.playerHiddenCardsFixed.setAdapter(playerHandAdapter);

        // Configurar RecyclerView de cartas en mesa
        LinearLayoutManager tableCardsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.tableCards.setLayoutManager(tableCardsManager);
        binding.tableCards.setAdapter(tableCardsAdapter);

        // Configurar RecyclerView de cartas descartadas en mesa
        LinearLayoutManager descartedCardsManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.descartedCards.setLayoutManager(descartedCardsManager);
        binding.descartedCards.setAdapter(discardedCardsAdapter);
    }

    private void setupClickListeners() {
        binding.btnSortHand.setOnClickListener(v -> {
            List<Card> currentHand = new ArrayList<>(playerHandAdapter.getCards());
            // Implementar lógica de ordenamiento
            playerHandAdapter.updateData(currentHand, true);
            Toast.makeText(this, "Cartas ordenadas", Toast.LENGTH_SHORT).show();
        });

        binding.btnPlaySelected.setOnClickListener(v -> {
            List<Card> selectedCards = playerHandAdapter.getSelectedCards();
            if (!selectedCards.isEmpty()) {
                viewModel.playCards(selectedCards);
                playerHandAdapter.clearSelection();
            }else{

            }
            //viewModel.nextTurn();
        });

        binding.btnSkipTurn.setOnClickListener(v -> {
            viewModel.nextTurn();
            Toast.makeText(this, "Turno pasado", Toast.LENGTH_SHORT).show();

        });

        binding.btnGameInfo.setOnClickListener(v -> showGameInfoDialog());
        //binding.btnRules.setOnClickListener(v -> showRulesDialog());
        binding.btnOptions.setOnClickListener(v -> showOptionsDialog());
    }
    private void loadPlayers(List<Player> players) {

        // Notificar al adapter que los datos han cambiado
        playerAdapter.updateData(players);
    }
    private void setupObservers() {
        viewModel.getPlayers().observe(this, players -> {
            if (players != null) {

                loadPlayers(players);
                playerAdapter.updateData(players);

                // Actualizar mano del jugador actual si existe
                Player currentPlayer = players.get(0);
                if (currentPlayer != null) {
                    playerHandAdapter.updateData(currentPlayer.getHand(), true);
                    binding.tvTableCardsHand.setText(currentPlayer.getHand().size() + "/104");
                }
            }
        });

        viewModel.getTableCards().observe(this, cards -> {
            if (cards != null) {
                tableCardsAdapter.updateData(cards, false);
                binding.tvTableCards.setText(cards.size() + "/104");
            }
        });

        viewModel.getDeck().observe(this, deck -> {
            if (deck != null) {
                binding.tvInitialDeck.setText(deck.size() + "/104");
            }
        });

        viewModel.getRemainingCards().observe(this, deck -> {
            if (deck != null) {
                binding.tvInitialDeck.setText(deck + "/104");
            }
        });

        viewModel.getGamePhase().observe(this, phase -> {
            if (phase != null) {
                binding.tvGameState.setText("Estado: " + phase);
                updateUIForGamePhase(phase);
            }
        });

        viewModel.getDeckDiscarded().observe(this, deck -> {
            if (deck != null) {
                discardedCardsAdapter.updateData(deck, false);
                binding.tvDescartedCards.setText(deck.size() + "/104");
            }
        });
    }

    private void updateUIForGamePhase(GamePhase phase) {
        // Actualizar UI basado en la fase del juego
        boolean isPlayerTurn = phase == GamePhase.PLAYER_TURN;
        //binding.btnPlaySelected.setEnabled(isPlayerTurn);
        //binding.btnSkipTurn.setEnabled(isPlayerTurn);
        //binding.btnSortHand.setEnabled(isPlayerTurn);

        // Actualizar adaptadores
        playerHandAdapter.setMultipleSelectionEnabled(isPlayerTurn);
    }

    private void startChronometer() {
        chronometer = binding.gameTimer;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void showGameInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Información de la Partida")
                .setMessage(String.format("Tiempo de juego: %s\nJugadores: %d\nCartas en mesa: %d",
                        chronometer.getText(),
                        playerAdapter.getItemCount(),
                        tableCardsAdapter.getItemCount()))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showRulesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reglas del Juego")
                .setMessage("1. Cada jugador recibe 5 cartas\n" +
                        "2. Las cartas se juegan en orden ascendente\n" +
                        "3. El 10 limpia la mesa\n" +
                        "4. Cuatro cartas iguales limpian la mesa")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showOptionsDialog() {
        String[] options = {"Abandonar partida", "Configurar sonido", "Ver estadísticas"};
        new AlertDialog.Builder(this)
                .setTitle("Opciones")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showExitConfirmationDialog();
                            break;
                        case 1:
                            Toast.makeText(this, "Configurando sonido...",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, "Mostrando estadísticas...",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Abandonar partida")
                .setMessage("¿Estás seguro que deseas abandonar la partida?")
                .setPositiveButton("Sí", (dialog, which) -> finish())
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