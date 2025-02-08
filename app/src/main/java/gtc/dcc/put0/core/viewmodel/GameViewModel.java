package gtc.dcc.put0.core.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.GamePhase;
import gtc.dcc.put0.core.model.Player;
import gtc.dcc.put0.core.utils.DeckUtils;

/**
 * ViewModel que maneja la lógica del juego de cartas.
 * Gestiona el estado del juego, los jugadores, el mazo y las cartas en la mesa.
 */
public class GameViewModel extends ViewModel {
    private static final int INITIAL_CARDS_PER_PLAYER = 7;

    // LiveData para el estado del juego
    private final MutableLiveData<List<Card>> _deck = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Card>> _deckDiscarded = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Player>> _players = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Card>> _tableCards = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> _currentPlayerIndex = new MutableLiveData<>(0);
    private final MutableLiveData<GamePhase> _gamePhase = new MutableLiveData<>();
    private final MutableLiveData<Integer> _remainingCards = new MutableLiveData<>(0);

    // Getters públicos para LiveData
    public LiveData<List<Card>> getDeck() { return _deck; }
    public LiveData<List<Card>> getDeckDiscarded() { return _deckDiscarded; }
    public LiveData<List<Player>> getPlayers() { return _players; }
    public LiveData<List<Card>> getTableCards() { return _tableCards; }
    public LiveData<Integer> getCurrentPlayerIndex() { return _currentPlayerIndex; }
    public LiveData<GamePhase> getGamePhase() { return _gamePhase; }
    public LiveData<Integer> getRemainingCards() { return _remainingCards; }

    /**
     * Obtiene el jugador actual basado en el índice del turno.
     * @return Player jugador actual o null si no hay jugadores
     */
    public Player getCurrentPlayer() {
        List<Player> players = _players.getValue();
        Integer currentIndex = _currentPlayerIndex.getValue();

        if (players != null && currentIndex != null && currentIndex < players.size()) {
            return players.get(currentIndex);
        }
        return null;
    }

    /**
     * Inicializa el juego con el número especificado de jugadores.
     * @param numPlayers número de jugadores
     * @param context contexto de la aplicación
     */
    public void initializeGame(int numPlayers, Context context) {
        if (numPlayers < 2) throw new IllegalArgumentException("Se requieren al menos 2 jugadores");

        initializeDeck(context);
        initializePlayers(numPlayers);
        dealInitialCards();

        _gamePhase.setValue(GamePhase.PLAYER_TURN);
        updateRemainingCards();
    }

    /**
     * Procesa el intento de jugar una carta.
     * @param card carta a jugar
     */
    public void playCard(Card card) {
        Player currentPlayer = getCurrentPlayer();
        if (!isValidPlayAttempt(currentPlayer, card)) return;

        List<Card> tableCards = new ArrayList<>(_tableCards.getValue() != null ?
                _tableCards.getValue() : new ArrayList<>());

        if (isValidPlay(card, tableCards)) {
            processValidPlay(card, currentPlayer, tableCards);
        }
    }

    /**
     * Procesa el intento de jugar múltiples cartas.
     * @param cards lista de cartas a jugar
     */
    // En GameViewModel.java
    /**
     * Procesa el intento de jugar múltiples cartas.
     */
    public void playCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return;

        List<Player> players = new ArrayList<>(_players.getValue());
        if (players == null || players.isEmpty()) return;

        Integer currentIndex = _currentPlayerIndex.getValue();
        if (currentIndex == null) return;

        Player currentPlayer = players.get(currentIndex);

        // Procesar cada carta
        for (Card card : cards) {
            if (currentPlayer.getHand().contains(card)) {
                currentPlayer.removeCardFromHand(card);

                // Robar nueva carta si hay disponibles
                List<Card> deck = _deck.getValue();
                if (deck != null && !deck.isEmpty()) {
                    currentPlayer.addCardToHand(deck.remove(0));
                    _deck.setValue(deck);
                    updateRemainingCards();
                }

                // Actualizar cartas en mesa
                List<Card> tableCards = new ArrayList<>(_tableCards.getValue() != null ?
                        _tableCards.getValue() : new ArrayList<>());
                tableCards.add(card);
                _tableCards.setValue(tableCards);

                if (shouldClearTable(tableCards)) {
                    clearTableAndUpdateDiscarded(tableCards);
                }
            }
        }

        // Actualizar el jugador en la lista
        players.set(currentIndex, currentPlayer);

        // Forzar actualización de la lista de jugadores
        _players.setValue(new ArrayList<>(players));
    }

    /**
     * Avanza al siguiente turno en el juego.
     */
    public void nextTurn() {
        List<Player> players = _players.getValue();
        Integer currentIndex = _currentPlayerIndex.getValue();

        if (currentIndex != null && players != null && !players.isEmpty()) {
            // Crear una nueva lista para evitar problemas de referencia
            List<Player> updatedPlayers = new ArrayList<>(players);

            // Desactivar el turno del jugador actual
            Player currentPlayer = updatedPlayers.get(currentIndex);
            currentPlayer.setIsCurrentTurn(false);

            // Calcular el índice del siguiente jugador
            int nextIndex = (currentIndex + 1) % players.size();

            // Activar el turno del siguiente jugador
            Player nextPlayer = updatedPlayers.get(nextIndex);
            nextPlayer.setIsCurrentTurn(true);

            // Actualizar el índice y la fase del juego
            _currentPlayerIndex.setValue(nextIndex);
            _gamePhase.setValue(nextIndex == 0 ? GamePhase.PLAYER_TURN : GamePhase.OPPONENT_TURN);

            // Forzar la actualización de la lista de jugadores
            _players.setValue(updatedPlayers);

            // Debug - imprimir estado
            for (Player p : updatedPlayers) {
                System.out.println("Player: " + p.getNames() + " isCurrentTurn: " + p.isCurrentTurn());
            }

        }
    }

    // Métodos privados de ayuda

    private void initializeDeck(Context context) {
        List<Card> mainDeck = new ArrayList<>();
        mainDeck.addAll(DeckUtils.createDeck(context, false));
        mainDeck.addAll(DeckUtils.createDeck(context, false));
        DeckUtils.shuffleDeck(mainDeck);
        _deck.setValue(mainDeck);
    }

    private void initializePlayers(int numPlayers) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            Player player = new Player("Player " + (i + 1));
            player.setCurrentTurn(i);
            player.setIsCurrentTurn(i == 0);
            players.add(player);
        }
        _players.setValue(players);
    }

    private void dealInitialCards() {
        List<Player> players = _players.getValue();
        List<Card> deck = _deck.getValue();

        if (players == null || deck == null) return;

        for (Player player : players) {
            dealPlayerCards(player, deck);
        }

        _deck.setValue(deck);
        updateRemainingCards();
        _players.setValue(players);
    }

    private void dealPlayerCards(Player player, List<Card> deck) {
        for (int i = 0; i < 15; i++) {
            if (deck.isEmpty()) break;

            player.addCardToHand(deck.remove(0));
            player.addHiddenCards(deck.remove(0));
            player.addVisibleCards(deck.remove(0));
        }
    }

    private boolean isValidPlayAttempt(Player player, Card card) {
        return player != null && card != null && player.isCurrentTurn();
    }

    private boolean isValidPlay(Card card, List<Card> tableCards) {
        if (tableCards.isEmpty()) return true;

        Card lastCard = tableCards.get(tableCards.size() - 1);
        return DeckUtils.getCardValue(card.getValue()) >= DeckUtils.getCardValue(lastCard.getValue())
                || "10".equals(card.getValue())
                || "2".equals(card.getValue());
    }

    private void processValidPlay(Card card, Player currentPlayer, List<Card> tableCards) {
        currentPlayer.removeCardFromHand(card);
        drawCardForPlayer(currentPlayer);

        tableCards.add(card);
        _tableCards.setValue(tableCards);

        updatePlayerState(currentPlayer);

        if (shouldClearTable(tableCards)) {
            clearTableAndUpdateDiscarded(tableCards);
        }
    }

    private void drawCardForPlayer(Player player) {
        List<Card> deck = _deck.getValue();
        if (deck != null && !deck.isEmpty()) {
            player.addCardToHand(deck.remove(0));
            _deck.setValue(deck);
            updateRemainingCards();
        }
    }

    private boolean shouldClearTable(List<Card> tableCards) {
        if (tableCards.isEmpty()) return false;

        // Verificar si la última carta es un 10
        Card lastCard = tableCards.get(tableCards.size() - 1);
        if ("10".equals(lastCard.getValue())) return true;

        // Verificar si hay 4 cartas iguales
        if (tableCards.size() >= 4) {
            int count = 0;
            for (int i = tableCards.size() - 1; i >= tableCards.size() - 4; i--) {
                if (Objects.equals(tableCards.get(i).getValue(), lastCard.getValue())) {
                    count++;
                }
            }
            return count == 4;
        }

        return false;
    }

    private void clearTableAndUpdateDiscarded(List<Card> tableCards) {
        List<Card> currentDiscarded = _deckDiscarded.getValue();
        if (currentDiscarded == null) currentDiscarded = new ArrayList<>();

        currentDiscarded.addAll(tableCards);
        _deckDiscarded.setValue(currentDiscarded);

        tableCards.clear();
        _tableCards.setValue(tableCards);
    }

    private void updatePlayerState(Player player) {
        List<Player> players = new ArrayList<>(_players.getValue());
        int index = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getNames().equals(player.getNames())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            // Crear un nuevo objeto Player con los datos actualizados
            Player updatedPlayer = new Player(player.getNames());
            updatedPlayer.setCurrentTurn(player.getCurrentTurn());
            updatedPlayer.setIsCurrentTurn(player.isCurrentTurn());
            // Copiar resto de datos necesarios...

            players.set(index, updatedPlayer);
            _players.setValue(new ArrayList<>(players));
        }
    }

    private void updateRemainingCards() {
        List<Card> deck = _deck.getValue();
        _remainingCards.setValue(deck != null ? deck.size() : 0);
    }

    private void updatePlayerTurnStatus(List<Player> players, int activePlayerIndex) {
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setIsCurrentTurn(i == activePlayerIndex);
        }
        _players.setValue(players);
    }
}