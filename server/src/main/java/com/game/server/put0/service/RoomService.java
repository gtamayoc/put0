package com.game.server.put0.service;

import com.game.server.put0.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing rooms and lobbies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {
    
    private final GameEngine gameEngine;
    
    /**
     * Creates a new game room.
     * 
     * @param playerName Name of the player creating the room
     * @param botCount Number of AI bots to add
     * @return The game ID and player ID
     */
    public RoomCreationResult createRoom(String playerName, int botCount, com.game.server.put0.model.MatchMode mode) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        if (botCount < 0 || botCount > 3) {
            throw new IllegalArgumentException("Bot count must be between 0 and 3");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Match mode is required");
        }
        String gameId = UUID.randomUUID().toString();
        String playerId = UUID.randomUUID().toString();
        
        // Create game
        com.game.server.put0.model.GameState game = gameEngine.createGame(gameId);
        game.setMode(mode);
        
        // Add human player
        Player humanPlayer = new Player(playerId, playerName, false);
        gameEngine.addPlayer(gameId, humanPlayer);
        
        // Add AI bots
        for (int i = 0; i < botCount; i++) {
            String botId = UUID.randomUUID().toString();
            String botName = "Bot " + (i + 1);
            Player bot = new Player(botId, botName, true);
            gameEngine.addPlayer(gameId, bot);
        }
        
        log.info("Created room {} ({}) with player {} and {} bots", gameId, mode, playerName, botCount);
        
        return new RoomCreationResult(gameId, playerId);
    }
    
    /**
     * Joins an existing game room.
     */
    public String joinRoom(String gameId, String playerName) {
        String playerId = UUID.randomUUID().toString();
        
        Player player = new Player(playerId, playerName, false);
        gameEngine.addPlayer(gameId, player);
        
        log.info("Player {} joined room {}", playerName, gameId);
        
        return playerId;
    }
    
    /**
     * Starts a game.
     */
    public void startGame(String gameId) {
        gameEngine.startGame(gameId);
        log.info("Started game {}", gameId);
    }
    
    /**
     * Removes a player from a room.
     */
    public void leaveRoom(String gameId, String playerId) {
        gameEngine.removePlayer(gameId, playerId);
    }
    
    /**
     * Result of room creation.
     */
    public record RoomCreationResult(String gameId, String playerId) {}
}
