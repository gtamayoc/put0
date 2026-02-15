package com.game.core.engine;

import com.game.core.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core game engine for PUT0.
 * Manages game rules, card dealing, and turn processing.
 * Stateless: Operates on a provided GameState.
 */
@Slf4j
public class GameEngine {

    /**
     * Initializes a new game state.
     */
    public GameState createGame(String gameId) {
        GameState game = new GameState(gameId);
        log.info("Created new game: {}", gameId);
        return game;
    }

    /**
     * Adds a player to the game.
     */
    public void addPlayer(GameState game, Player player) {
        synchronized (game) {
            if (game.getStatus() != GameStatus.WAITING) {
                throw new IllegalStateException("Cannot add player to game in progress");
            }

            game.addPlayer(player);
            log.info("Added player {} to game {}", player.getName(), game.getGameId());
        }
    }

    /**
     * Starts a game by creating and dealing the deck.
     */
    public void startGame(GameState game) {
        synchronized (game) {
            if (game.getStatus() == GameStatus.PLAYING) {
                log.info("Game {} already started, ignoring start request", game.getGameId());
                return;
            }

            if (!game.canStart()) {
                throw new IllegalStateException("Game cannot start - need at least 2 players");
            }

            // Create and shuffle deck (Configurable size: 52 or 104)
            game.setMainDeck(createDeck(game.getDeckSize()));
            Collections.shuffle(game.getMainDeck());

            // Deal cards according to rules: 3 Hidden, 3 Visible, 3 Hand
            dealCards(game);

            game.setStatus(GameStatus.PLAYING);
            log.info("Started game {} with {} players", game.getGameId(), game.getPlayers().size());
        }
    }

    /**
     * Creates a deck of cards.
     * 
     * @param size total number of cards (52 or 104).
     */
    private List<Card> createDeck(int size) {
        List<Card> deck = new ArrayList<>();
        // number of decks: 52 -> 1 deck, 104 -> 2 decks
        int deckCount = (size <= 52) ? 1 : 2;

        for (int i = 0; i < deckCount; i++) {
            for (Suit suit : Suit.values()) {
                for (int value = 1; value <= 13; value++) {
                    deck.add(new Card(value, suit));
                }
            }
        }
        return deck;
    }

    /**
     * Deals 3 cards Hidden, 3 Visible, 3 to Hand for each player.
     */
    private void dealCards(GameState game) {
        List<Card> deck = game.getMainDeck();
        List<Player> players = game.getPlayers();

        // 1. Deal 3 Hidden Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) {
                    Card hiddenCard = deck.remove(deck.size() - 1);
                    hiddenCard.setHidden(true); // VERY IMPORTANT: Mark as hidden
                    player.getHiddenCards().add(hiddenCard);
                }
            }
        }

        // 2. Deal 3 Visible Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty())
                    player.getVisibleCards().add(deck.remove(deck.size() - 1));
            }
        }

        // 3. Deal 3 Hand Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty())
                    player.getHand().add(deck.remove(deck.size() - 1));
            }
        }

        log.info("Dealt initial hands (3+3+3) to {} players", players.size());
    }

    /**
     * Plays a card from the current player's hand.
     */
    public void playCard(GameState game, String playerId, Card card) {
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("Game is not in progress");
        }

        synchronized (game) {
            Player currentPlayer = game.getCurrentPlayer();
            if (!currentPlayer.getId().equals(playerId)) {
                throw new IllegalStateException("Not this player's turn");
            }

            // Validate card can be played AND is reachable in current phase
            Card topCard = game.getTopCard();

            // Strict Phase Validation: Player can only play what getPlayableCards returns
            List<Card> validMoves = currentPlayer.getPlayableCards(topCard);

            // Note: contains() checks equality. With 2 decks, identical cards exist.
            if (!validMoves.contains(card)) {
                log.warn("[GAME-INVALID] Player {} tried to play {} but it's not in valid moves. Valid: {}",
                        currentPlayer.getName(), card, validMoves);
                throw new IllegalArgumentException("Invalid move: Card not available or not playable in current phase");
            }

            // Track if the card was hidden BEFORE we potentially reveal it
            boolean wasHiddenAtStart = card.isHidden();

            // Reveal hidden card if played
            if (wasHiddenAtStart) {
                card.setHidden(false);
                // Ensure instance in hand is also revealed
                currentPlayer.getHand().stream()
                        .filter(c -> c.equals(card))
                        .findFirst()
                        .ifPresent(c -> c.setHidden(false));
            }

            // Check if card is actually playable on top card
            if (!card.canPlayOn(topCard)) {
                // FAILED PLAY
                if (wasHiddenAtStart) {
                    // Phase 4: Blind discovery failure - Penalize with Eat Table
                    log.info("[GAME-ACTION] Player {} FAILED blind play of {} on {}. Penalty: Eat Table.",
                            currentPlayer.getName(), card, topCard);

                    // Regression logic for Phase 4:
                    // If player picks up the table, ALL hidden cards must remain hidden.
                    // If they were somehow in the hand, they should go back to the hidden pile.
                    List<Card> hiddenInHand = currentPlayer.getHand().stream()
                            .filter(Card::isHidden)
                            .filter(c -> !c.equals(card))
                            .toList();

                    if (!hiddenInHand.isEmpty()) {
                        currentPlayer.getHiddenCards().addAll(hiddenInHand);
                        currentPlayer.getHand().removeAll(hiddenInHand);
                    }

                    // Double check all cards in hiddenCards are definitely marked as hidden
                    for (Card c : currentPlayer.getHiddenCards()) {
                        c.setHidden(true);
                    }

                    currentPlayer.removeCard(card);
                    card.setHidden(false); // Ensure it is definitely revealed
                    game.getTablePile().add(card);

                    game.collectTable(currentPlayer);

                    // Safety: After collecting, find THIS specific card in hand and ensure it's not
                    // hidden
                    // This covers the case where duplicates might exist in the hand
                    currentPlayer.getHand().stream()
                            .filter(c -> c.equals(card))
                            .forEach(c -> c.setHidden(false));

                    String msg = String.format("Has descubierto un %s. Al no superar la mesa, recoges las cartas.",
                            card.toString());
                    game.setLastAction(msg);
                    game.nextTurn();
                    return;
                } else {
                    // For hand or visible cards (ALREADY REVEALED), just block the invalid move
                    // This prevents player from "eating table" accidentally on a misplay.
                    throw new IllegalArgumentException("Esta carta no se puede jugar sobre un "
                            + (topCard != null ? topCard.getPower() : "mesa vacía"));
                }
            }

            // Remove card from player's hand (managed by Player logic)
            if (!currentPlayer.removeCard(card)) {
                log.error("[GAME-ERROR] Could not remove card {} from player {}", card, currentPlayer.getName());
                throw new IllegalArgumentException("Player does not have this card");
            }

            // Add card to table
            game.getTablePile().add(card);
            String msg = String.format("Player %s PLAYED %s on top of %s.", currentPlayer.getName(), card, topCard);
            log.info("[GAME-ACTION] " + msg);
            game.setLastAction(msg);

            // Check for table clear conditions
            if (card.clearsTable() || game.shouldClearTable()) {
                game.clearTable();
                String clearMsg = String.format("Table CLEARED by %s.", currentPlayer.getName());
                log.info("[GAME-EVENT] " + clearMsg);
                game.setLastAction(clearMsg);

                // Replenish hand before playing again (Phase 1)
                replenishHand(game, currentPlayer);
                // Player goes again (no nextTurn())
                return;
            }

            // Replenish hand from mainDeck if needed (Phase 1)
            replenishHand(game, currentPlayer);

            // Check if player won
            if (currentPlayer.hasWon()) {
                game.setStatus(GameStatus.FINISHED);
                game.setWinnerId(playerId);
                log.info("[GAME-OVER] Player {} WON the game!", currentPlayer.getName());
                return;
            }

            // Move to next player
            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }

    /**
     * Plays multiple cards from the current player's hand.
     * All cards must be of the SAME RANK.
     * The first card must be playable on the table.
     */
    public void playCards(GameState game, String playerId, List<Card> cards) {
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("Game is not in progress");
        }
        if (cards == null || cards.isEmpty()) {
            return;
        }

        // Single card fallback
        if (cards.size() == 1) {
            playCard(game, playerId, cards.get(0));
            return;
        }

        synchronized (game) {
            Player currentPlayer = game.getCurrentPlayer();
            if (!currentPlayer.getId().equals(playerId)) {
                throw new IllegalStateException("Not this player's turn");
            }

            // 1. Validate "Same Rank" constraint
            int firstRank = cards.get(0).getValue();
            for (Card c : cards) {
                if (c.getValue() != firstRank) {
                    throw new IllegalArgumentException("All cards must check the same rank");
                }
            }

            // 2. Validate first card against Top Card
            Card topCard = game.getTopCard();
            Card firstCard = cards.get(0);

            // Phase/Availability validation
            List<Card> validMoves = currentPlayer.getPlayableCards(topCard);

            // Check if ALL cards are actually in hand/playable
            for (Card c : cards) {
                if (!validMoves.contains(c)) {
                    log.warn("[GAME-INVALID] Player {} tried to play {} but it's not in valid moves.",
                            currentPlayer.getName(), c);
                    throw new IllegalArgumentException("One or more cards are not available to play");
                }
            }

            // Check gameplay rule (Can play on top?)
            if (!firstCard.canPlayOn(topCard)) {
                // For multiple throw, we assume they are from hand (not blind).
                // Blind play logic (Phase 4) usually involves single cards.
                // If specific logic for blind is needed, it defaults to single play.
                throw new IllegalArgumentException(
                        "Cannot play " + firstCard + " on " + (topCard != null ? topCard : "empty table"));
            }

            // 3. Execution Loop
            // We strip them from hand and add to table, but only switch turn at the end.
            StringBuilder actionMsg = new StringBuilder();
            actionMsg.append(String.format("Player %s PLAYED %d cards (", currentPlayer.getName(), cards.size()));

            boolean clearedTable = false;

            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);

                // Remove from player
                if (!currentPlayer.removeCard(c)) {
                    log.error("[GAME-ERROR] Could not remove card {} during multi-play", c);
                    continue;
                }

                // Add to table
                game.getTablePile().add(c);
                actionMsg.append(c.toString()).append(i < cards.size() - 1 ? ", " : "");

                // Check Clear (Last card usually dictates, or any card that clears?)
                // If a 2 or 10 is played, it clears. If multiple 3s are played, they stack.
                if (c.clearsTable() || game.shouldClearTable()) {
                    clearedTable = true;
                    // Usually if table acts cleared, we stop adding?
                    // But user wants to throw ALL. If I throw '2' and '2', first one clears...
                    // Wait, '2' clears table. You wouldn't throw multiple '2's usually (waste).
                    // But if you throw three '3's, and table has '1', it's fine.
                }
            }

            actionMsg.append(") on top of ").append(topCard);
            log.info("[GAME-ACTION] " + actionMsg.toString());
            game.setLastAction(actionMsg.toString());

            if (clearedTable) {
                game.clearTable();
                String clearMsg = String.format("Table CLEARED by %s (Multi-throw).", currentPlayer.getName());
                log.info("[GAME-EVENT] " + clearMsg);
                game.setLastAction(clearMsg);

                replenishHand(game, currentPlayer);
                return; // Keep turn
            }

            // Replenish hand
            replenishHand(game, currentPlayer);

            // Check Win
            if (currentPlayer.hasWon()) {
                game.setStatus(GameStatus.FINISHED);
                game.setWinnerId(playerId);
                log.info("[GAME-OVER] Player {} WON the game!", currentPlayer.getName());
                return;
            }

            // Next Turn
            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }

    /**
     * Replenishes player's hand to 3 cards if deck is available (Phase 1).
     */
    private void replenishHand(GameState game, Player player) {
        // Phase 1: Draw from main deck (3 cards target)
        while (player.getHand().size() < 3 && !game.getMainDeck().isEmpty()) {
            player.getHand().add(game.getMainDeck().remove(game.getMainDeck().size() - 1));
        }

        // Transition to Phase 3 (Visible Cards to hand)
        if (player.getHand().isEmpty() && game.getMainDeck().isEmpty() && !player.getVisibleCards().isEmpty()) {
            log.info("[GAME-PHASE] Transitioning player {} to Visible Cards phase (F3).", player.getName());
            player.getHand().addAll(player.getVisibleCards());
            player.getVisibleCards().clear();
        }

        // Transition to Phase 4 (Hidden Cards)
        // Note: We no longer move them to hand automatically.
        // They stay in the hidden pile and are played from there.
        if (player.getHand().isEmpty() && game.getMainDeck().isEmpty() &&
                player.getVisibleCards().isEmpty() && !player.getHiddenCards().isEmpty()) {
            // Just ensure they are definitely marked as hidden
            for (Card c : player.getHiddenCards()) {
                c.setHidden(true);
            }
        }
    }

    /**
     * Draws a card from the deck for the current player.
     * Used when player has no valid moves or chooses to pick up.
     */
    public void drawCard(GameState game, String playerId) {
        synchronized (game) {
            Player currentPlayer = game.getCurrentPlayer();
            if (!currentPlayer.getId().equals(playerId)) {
                throw new IllegalStateException("Not this player's turn");
            }

            // If mainDeck is not empty -> Draw one card (Phase 1)
            if (!game.getMainDeck().isEmpty()) {
                Card drawnCard = game.getMainDeck().remove(game.getMainDeck().size() - 1);
                currentPlayer.addCard(drawnCard);
                String msg = String.format("Player %s DREW a card.", currentPlayer.getName());
                log.info("[GAME-ACTION] " + msg);
                game.setLastAction(msg);
                game.nextTurn();
                log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(),
                        game.getCurrentPlayerIndex());
                return;
            }

            // If undefined play or Phase transition logic requires picking up table
            // When deck is empty and player cannot play, they must pick up the Table Pile
            int tableSize = game.getTablePile().size();
            game.collectTable(currentPlayer);
            String msg = String.format("Player %s collected %d cards from table (Deck empty).", currentPlayer.getName(),
                    tableSize);
            log.info("[GAME-ACTION] " + msg);
            game.setLastAction(msg);

            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }

    /**
     * Helper to collect table cards (Passive/Voluntary action).
     * Used when player chooses to pick up the table instead of playing.
     */
    public void collectTable(GameState game, String playerId) {
        synchronized (game) {
            Player currentPlayer = game.getCurrentPlayer();
            if (!currentPlayer.getId().equals(playerId)) {
                throw new IllegalStateException("Not this player's turn to collect");
            }

            if (game.getTablePile().isEmpty()) {
                throw new IllegalStateException("Table is empty, nothing to collect");
            }

            // Perform collection using GameState's logic
            int tableSize = game.getTablePile().size();
            game.collectTable(currentPlayer);
            String msg = String.format("Player %s collected %d cards (voluntary).", currentPlayer.getName(), tableSize);
            log.info("[GAME-ACTION] " + msg);
            game.setLastAction(msg);

            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }
}
