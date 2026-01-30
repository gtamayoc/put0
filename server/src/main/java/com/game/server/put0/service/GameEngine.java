package com.game.server.put0.service;

import com.game.core.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Game Engine Wrapper.
 * Manages the collection of active games and delegates logic to the Core GameEngine.
 */
@Service
public class GameEngine {
    
    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);
    
    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final com.game.core.engine.GameEngine coreEngine = new com.game.core.engine.GameEngine();
    
    /**
     * Creates a new game with the given ID.
     */
    public GameState createGame(String gameId) {
        GameState game = coreEngine.createGame(gameId);
        games.put(gameId, game);
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
        coreEngine.addPlayer(game, player);
    }
    
    /**
     * Starts a game by creating and dealing the deck.
     */
    public void startGame(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        coreEngine.startGame(game);
    }
    
    /**
     * Plays a card from the current player's hand.
     */
    public void playCard(String gameId, String playerId, Card card) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        coreEngine.playCard(game, playerId, card);
    }
    
    /**
     * Draws a card from the deck for the current player.
     */
    public void drawCard(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        coreEngine.drawCard(game, playerId);
    }
    
    /**
     * Collects table cards (Voluntary action).
     */
    public void collectTable(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        coreEngine.collectTable(game, playerId);
    }
    
    /**
     * Gets all active games.
     */
    public Collection<GameState> getAllGames() {
        return games.values();
    }
    
    /**
     * Removes a player from the game.
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
                 if (game.getStatus() == GameStatus.PLAYING && game.getCurrentPlayerIndex() >= game.getPlayers().size()) {
                      game.setCurrentPlayerIndex(0); 
                 }
            }
        }
    }
    
    public void removeGame(String gameId) {
        games.remove(gameId);
        log.info("Removed game {}", gameId);
    }

    // Expose core engine if needed (e.g. for bot)
    public com.game.core.engine.GameEngine getCore() {
        return coreEngine;
    }
}
