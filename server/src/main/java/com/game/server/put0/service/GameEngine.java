package com.game.server.put0.service;

import com.game.server.put0.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game engine for PUT0.
 * Manages game state, deck creation, card dealing, and game rules.
 */
@Service
@Slf4j
public class GameEngine {
    
    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    
    /**
     * Creates a new game with the given ID.
     */
    public GameState createGame(String gameId) {
        GameState game = new GameState(gameId);
        games.put(gameId, game);
        log.info("Created new game: {}", gameId);
        return game;
    }
    
    /**
     * Gets a game by ID.
     */
    public GameState getGame(String gameId) {
        return games.get(gameId);
    }
    
    /**
     * Adds a player to a game.
     */
    public void addPlayer(String gameId, Player player) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        synchronized(game) {
            if (game.getStatus() != GameStatus.WAITING) {
                throw new IllegalStateException("Cannot add player to game in progress");
            }
            
            game.addPlayer(player);
            log.info("Added player {} to game {}", player.getName(), gameId);
        }
    }
    
    /**
     * Starts a game by creating and dealing the deck.
     */
    public void startGame(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        synchronized(game) {
            if (game.getStatus() == GameStatus.PLAYING) {
                log.info("Game {} already started, ignoring start request", gameId);
                return;
            }
            
            if (!game.canStart()) {
                throw new IllegalStateException("Game cannot start - need at least 2 players");
            }
            
            // Create and shuffle deck (2 Decks = 104 Cards)
            game.setMainDeck(createDeck());
            Collections.shuffle(game.getMainDeck());
            
            // Deal cards according to rules: 3 Hidden, 3 Visible, 3 Hand
            dealCards(game);
            
            game.setStatus(GameStatus.PLAYING);
            log.info("Started game {} with {} players", gameId, game.getPlayers().size());
        }
    }
    
    /**
     * Creates a double deck (104 cards).
     */
    private List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();
        // 2 Decks
        for (int i = 0; i < 2; i++) {
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
                if (!deck.isEmpty()) player.getHiddenCards().add(deck.remove(deck.size() - 1));
            }
        }

        // 2. Deal 3 Visible Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) player.getVisibleCards().add(deck.remove(deck.size() - 1));
            }
        }

        // 3. Deal 3 Hand Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) player.getHand().add(deck.remove(deck.size() - 1));
            }
        }
        
        log.info("Dealt initial hands (3+3+3) to {} players", players.size());
    }
    
    /**
     * Plays a card from the current player's hand.
     */
    public void playCard(String gameId, String playerId, Card card) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("Game is not in progress");
        }
        
        synchronized(game) {
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
                log.warn("[GAME-INVALID] Player {} tried to play {} but it's not in valid moves. Valid: {}", currentPlayer.getName(), card, validMoves);
                throw new IllegalArgumentException("Invalid move: Card not available or not playable in current phase");
            }
            
            // Reveal hidden card if played
            if (card.isHidden()) {
                card.setHidden(false);
            }

            // Check if card is actually playable on top card
            if (!card.canPlayOn(topCard)) {
                // FAILED PLAY (Phase 3 or 4 failure)
                log.info("[GAME-ACTION] Player {} FAILED to play {} on top card {}. Penalty: Eat Table.", currentPlayer.getName(), card, topCard);
                
                // If this was Phase 4 (hand had other hidden cards), move them back to hiddenCards pile (Regression)
                List<Card> remainingHidden = currentPlayer.getHand().stream()
                        .filter(Card::isHidden)
                        .toList();
                
                if (!remainingHidden.isEmpty()) {
                    log.info("[GAME-REGRESSION] Player {} failed blind play. Suspending Phase 4. Moving {} cards back to hidden pile.", 
                            currentPlayer.getName(), remainingHidden.size());
                    currentPlayer.getHiddenCards().addAll(remainingHidden);
                    currentPlayer.getHand().removeIf(Card::isHidden);
                }

                // Remove the failed card from player's hand/hidden pile
                currentPlayer.removeCard(card);
                
                // Add the failed card to the table before collecting
                game.getTablePile().add(card);
                
                int tableSizeBefore = game.getTablePile().size();
                // Rules: Player collects all table cards
                game.collectTable(currentPlayer);
                log.info("[GAME-PENALTY] Player {} collected {} cards from table.", currentPlayer.getName(), tableSizeBefore);
                
                game.nextTurn();
                log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
                return;
            }

            // Remove card from player's hand (managed by Player logic)
            if (!currentPlayer.removeCard(card)) {
                log.error("[GAME-ERROR] Could not remove card {} from player {}", card, currentPlayer.getName());
                throw new IllegalArgumentException("Player does not have this card");
            }
            
            // Add card to table
            game.getTablePile().add(card);
            log.info("[GAME-ACTION] Player {} PLAYED {} on top of {}. Table size: {}", currentPlayer.getName(), card, topCard, game.getTablePile().size());
            
            // Check for table clear conditions
            if (card.clearsTable() || game.shouldClearTable()) {
                game.clearTable();
                log.info("[GAME-EVENT] Table CLEARED by {}. {} goes again.", currentPlayer.getName(), currentPlayer.getName());
                
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

        // Transition to Phase 4 (Hidden Cards to hand)
        // Happens only when hand, deck, AND visible cards are exhausted
        if (player.getHand().isEmpty() && game.getMainDeck().isEmpty() && 
            player.getVisibleCards().isEmpty() && !player.getHiddenCards().isEmpty()) {
            log.info("[GAME-PHASE] Transitioning player {} to Hidden Cards phase (F4). Moving {} cards.", 
                    player.getName(), player.getHiddenCards().size());
            
            // Move ALL hidden cards to hand and mark them as hidden for blind play
            for (Card card : player.getHiddenCards()) {
                card.setHidden(true);
                player.getHand().add(card);
            }
            player.getHiddenCards().clear();
        }
    }

    /**
     * Draws a card from the deck for the current player.
     * Used when player has no valid moves or chooses to pick up.
     */
    public void drawCard(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        synchronized(game) {
            Player currentPlayer = game.getCurrentPlayer();
            if (!currentPlayer.getId().equals(playerId)) {
                throw new IllegalStateException("Not this player's turn");
            }
            
            // If mainDeck is not empty -> Draw one card (Phase 1)
            if (!game.getMainDeck().isEmpty()) {
                 Card drawnCard = game.getMainDeck().remove(game.getMainDeck().size() - 1);
                 currentPlayer.addCard(drawnCard);
                 log.info("[GAME-ACTION] Player {} DREW a card. Deck remaining: {}", currentPlayer.getName(), game.getMainDeck().size());
                 game.nextTurn();
                 log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
                 return;
            }
            
            // If undefined play or Phase transition logic requires picking up table
            // When deck is empty and player cannot play, they must pick up the Table Pile
            int tableSize = game.getTablePile().size();
            game.collectTable(currentPlayer);
            log.info("[GAME-ACTION] Player {} collected {} cards from table (Deck empty).", currentPlayer.getName(), tableSize);
            
            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }

    /**
     * Helper to collect table cards (Passive/Voluntary action).
     * Used when player chooses to pick up the table instead of playing.
     */
    public void collectTable(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        synchronized(game) {
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
            log.info("[GAME-ACTION] Player {} collected {} cards (voluntary).", currentPlayer.getName(), tableSize);

            game.nextTurn();
            log.info("[GAME-TURN] Next turn: {} ({})", game.getCurrentPlayer().getName(), game.getCurrentPlayerIndex());
        }
    }
    
    /**
     * Gets all active games.
     */
    public Collection<GameState> getAllGames() {
        return games.values();
    }
    
    /**
     * Removes a player from the game.
     * If the game becomes empty, it is removed.
     */
    public void removePlayer(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            return;
        }
        
        synchronized(game) {
            game.getPlayers().removeIf(p -> p.getId().equals(playerId));
            log.info("Removed player {} from game {}", playerId, gameId);
            
            if (game.getPlayers().isEmpty() || game.getPlayers().stream().allMatch(Player::isBot)) {
                removeGame(gameId);
            } else {
                // Should we notify others? handled by controller/websocket usually
                // If the game was running, we might need to adjust current player index
                if (game.getStatus() == GameStatus.PLAYING && game.getCurrentPlayerIndex() >= game.getPlayers().size()) {
                     game.setCurrentPlayerIndex(0); // Reset or handle appropriately
                }
            }
        }
    }

    /**
     * Removes a game.
     */
    public void removeGame(String gameId) {
        games.remove(gameId);
        log.info("Removed game {}", gameId);
    }
}
