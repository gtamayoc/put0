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
        
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Cannot add player to game in progress");
        }
        
        game.addPlayer(player);
        log.info("Added player {} to game {}", player.getName(), gameId);
    }
    
    /**
     * Starts a game by creating and dealing the deck.
     */
    public void startGame(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        if (game.getStatus() == GameStatus.PLAYING) {
            log.info("Game {} already started, ignoring start request", gameId);
            return;
        }
        
        if (!game.canStart()) {
            throw new IllegalStateException("Game cannot start - need at least 2 players");
        }
        
        // Create and shuffle deck (2 Decks = 104 Cards)
        game.setDeck(createDeck());
        Collections.shuffle(game.getDeck());
        
        // Deal cards according to rules: 3 Hidden, 3 Visible, 3 Hand
        dealCards(game);
        
        game.setStatus(GameStatus.PLAYING);
        log.info("Started game {} with {} players", gameId, game.getPlayers().size());
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
        List<Card> deck = game.getDeck();
        List<Player> players = game.getPlayers();
        
        // 1. Deal 3 Hidden Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) player.getHiddenCards().add(deck.remove(0));
            }
        }

        // 2. Deal 3 Visible Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) player.getVisibleCards().add(deck.remove(0));
            }
        }

        // 3. Deal 3 Hand Cards
        for (int i = 0; i < 3; i++) {
            for (Player player : players) {
                if (!deck.isEmpty()) player.getHand().add(deck.remove(0));
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
             throw new IllegalArgumentException("Invalid move: Card not available or not playable in current phase (Hand -> Visible -> Hidden)");
        }
        
        // Check if card is actually playable on top card
        // For phases 1 and 2, validMoves only contains playable cards.
        // For phase 3 (Hidden), validMoves contains ALL hidden cards, so we must check here.
        if (!card.canPlayOn(topCard)) {
            // FAILED PLAY (Phase 3 Hidden Card failure)
            log.info("Player {} failed to play {} on top card {}", playerId, card, topCard);
            
            // Remove the card from player's hidden pile
            currentPlayer.removeCard(card);
            
            // Add the failed card to the table before collecting
            game.getTable().add(card);
            
            // Rules say: "Si la carta robada es menor... debe recoger todas las cartas visibles"
            game.collectTable(currentPlayer);
            
            game.nextTurn();
            return;
        }

        // Remove card from player's hand (managed by Player logic)
        if (!currentPlayer.removeCard(card)) {
            // Should be covered by above check, but safety net
            throw new IllegalArgumentException("Player does not have this card");
        }
        
        // Add card to table
        game.getTable().add(card);
        log.info("Player {} played {} on game {}", playerId, card, gameId);
        
        // Check for table clear conditions
        if (card.clearsTable() || game.shouldClearTable()) {
            game.clearTable();
            log.info("Table cleared in game {} by {}", gameId, card);
            // Same player plays again after clearing? 
            // Rules say: "Se juega una nueva carta para reiniciar". Yes, player goes again.
            // Also "Robo extra con 10". If implemented, handle here.
            
            // Auto-draw if needed before replay?
            replenishHand(game, currentPlayer);
            return;
        }
        
        // Replenish hand from deck if needed (Target 3 cards in hand)
        replenishHand(game, currentPlayer);

        // Check if player won
        if (currentPlayer.hasWon()) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerId(playerId);
            log.info("Player {} won game {}", playerId, gameId);
            return;
        }
        
        // Move to next player
        game.nextTurn();
    }
    
    /**
     * Replenishes player's hand to 3 cards if deck is available.
     * Only applies if playing from Hand phase (detected if hand < 3).
     */
    private void replenishHand(GameState game, Player player) {
        // Only draw if we are still using the deck? 
        // Rules: "Se roba una carta después de jugar (mientras haya disponibles)"
        // Typically you maintain 3 cards in hand.
        while (player.getHand().size() < 3 && !game.getDeck().isEmpty()) {
            player.getHand().add(game.getDeck().remove(0));
        }
    }

    /**
     * Draws a card from the deck for the current player.
     * Used when player has no valid moves.
     */
    public void drawCard(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        Player currentPlayer = game.getCurrentPlayer();
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        
        // Check if player has any playable cards
        List<Card> playableCards = currentPlayer.getPlayableCards(game.getTopCard());
        if (!playableCards.isEmpty()) {
            // Technically, if they have hidden cards only, they assume risk. 
            // But if they have Hand/Visible playable, they shouldn't just draw blindly to avoid playing?
            // Actually, usually you draw if you CANNOT play.
            throw new IllegalStateException("Player has playable cards and cannot draw");
        }
        
        // Draw card from deck
        if (game.getDeck().isEmpty()) {
            // If deck is empty and you can't play... you pick up the table? 
            // Rules: "Si la carta oculta es menor... debe recoger todas las cartas visibles".
            // That applies to Hidden phase. 
            // What if stuck in Hand phase?
            // "Robo automático después de jugar".
            // Typically you pick up the pile if you can't play.
            game.collectTable(currentPlayer); // Need to implement this in GameState or here help
            
            game.nextTurn();
            return;
        }
        
        // Standard draw logic (one card? or until playable?)
        // Usually you draw one and if it works you play, else pass/collect.
        // For simple implementation: Draw one.
        Card drawnCard = game.getDeck().remove(0);
        currentPlayer.addCard(drawnCard);
        log.info("Player {} drew a card in game {}", playerId, gameId);
        
        // If the drawn card is playable, they can play it? 
        // Usually turn ends after draw unless specified.
        game.nextTurn();
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
        
        game.getPlayers().removeIf(p -> p.getId().equals(playerId));
        log.info("Removed player {} from game {}", playerId, gameId);
        
        if (game.getPlayers().isEmpty() || game.getPlayers().stream().allMatch(Player::isBot)) {
            removeGame(gameId);
        } else {
            // Should we notify others? handled by controller/websocket usually
            // If the game was running, we might need to adjust current player index
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
