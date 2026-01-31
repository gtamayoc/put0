package com.game.server.put0.service;

import com.game.core.bot.BotStrategy;
import com.game.core.bot.DefaultBotStrategy;
import com.game.core.model.GameState;
import com.game.core.model.Player;
import com.game.core.model.GameStatus;
import com.game.server.put0.dto.GameStateUpdate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIBotService {

    private static final Logger log = LoggerFactory.getLogger(AIBotService.class);

    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;
    // Core Bot Strategy
    private final BotStrategy botStrategy = new DefaultBotStrategy();

    public void makeMove(String gameId, String botPlayerId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) {
            log.error("Game not found: {}", gameId);
            return;
        }

        Player bot = game.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst()
                .orElse(null);

        if (bot == null || !bot.isBot()) {
            log.error("Bot player not found or not a bot: {}", botPlayerId);
            return;
        }

        // Execute move using Core Strategy against Core Engine (exposed by wrapper)
        try {
            botStrategy.playTurn(game, gameEngine.getCore(), bot);
        } catch (Exception e) {
            log.error("Bot strategy failed", e);
            return;
        }
        
        // Broadcast update based on Last Action recorded in GameState
        String lastAction = game.getLastAction();
        // Determine update type roughly
        GameStateUpdate.UpdateType type = GameStateUpdate.UpdateType.CARD_PLAYED; // Default
        if (lastAction != null) {
            if (lastAction.contains("collected")) type = GameStateUpdate.UpdateType.TABLE_COLLECTED;
            if (lastAction.contains("DREW")) type = GameStateUpdate.UpdateType.CARD_DRAWN;
        }
        
        broadcastUpdate(gameId, lastAction != null ? lastAction : "Bot moved", type);

        // Check for next turn (recursive bot turn if sequential)
        checkAndMakeBotMove(gameId);
    }
    
    @Async("gameExecutor")
    public void checkAndMakeBotMove(String gameId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) return;
        
        // Slight delay for UX
         try {
            Thread.sleep(700); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isBot() && game.getStatus() == GameStatus.PLAYING) {
             makeMove(gameId, currentPlayer.getId());
        }
    }

    private void broadcastUpdate(String gameId, String message, GameStateUpdate.UpdateType type) {
        GameState game = gameEngine.getGame(gameId);
        if (game != null) {
            GameStateUpdate update = new GameStateUpdate(game, message, type);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
        }
    }
}
