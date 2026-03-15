package com.game.server.put0.service;

import com.game.core.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class RoomService {

    private static final int MAX_BOT_COUNT = 3;
    private static final int MIN_BOT_COUNT = 0;

    private final GameEngine gameEngine;

    public RoomService(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public RoomCreationResult createRoom(String playerName, int botCount, com.game.core.model.MatchMode mode) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        if (botCount < MIN_BOT_COUNT || botCount > MAX_BOT_COUNT) {
            throw new IllegalArgumentException("Bot count must be between " + MIN_BOT_COUNT + " and " + MAX_BOT_COUNT);
        }
        if (mode == null) {
            throw new IllegalArgumentException("Match mode is required");
        }
        String gameId = UUID.randomUUID().toString();
        String playerId = UUID.randomUUID().toString();
        
        com.game.core.model.GameState game = gameEngine.createGame(gameId);
        game.setMode(mode);
        
        Player humanPlayer = new Player(playerId, playerName, false);
        gameEngine.addPlayer(gameId, humanPlayer);
        
        for (int i = 0; i < botCount; i++) {
            String botId = UUID.randomUUID().toString();
            String botName = "Bot " + (i + 1);
            Player bot = new Player(botId, botName, true);
            gameEngine.addPlayer(gameId, bot);
        }
        
        log.info("Created room gameId={} mode={} player={} botCount={}", gameId, mode, playerName, botCount);
        
        return new RoomCreationResult(gameId, playerId);
    }
    
    public String joinRoom(String gameId, String playerName) {
        String playerId = UUID.randomUUID().toString();
        
        Player player = new Player(playerId, playerName, false);
        gameEngine.addPlayer(gameId, player);
        
        log.info("Player joined playerName={} gameId={}", playerName, gameId);
        
        return playerId;
    }
    
    public void startGame(String gameId) {
        gameEngine.startGame(gameId);
        log.info("Game started gameId={}", gameId);
    }
    
    public void leaveRoom(String gameId, String playerId) {
        gameEngine.removePlayer(gameId, playerId);
    }
    
    public record RoomCreationResult(String gameId, String playerId) {}
}
