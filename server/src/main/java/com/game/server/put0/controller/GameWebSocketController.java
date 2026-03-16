package com.game.server.put0.controller;

import com.game.server.put0.dto.DrawCardRequest;
import com.game.server.put0.dto.GameStateUpdate;
import com.game.server.put0.dto.PlayCardRequest;
import com.game.core.model.GameState;
import com.game.server.put0.service.AIBotService;
import com.game.server.put0.service.GameEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for handling game actions.
 * Clients send messages to /app/* endpoints.
 * Server broadcasts updates to /topic/game/{gameId}.
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketController.class);
    
    private final GameEngine gameEngine;
    private final AIBotService aiBotService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handles play card action from client.
     * Client sends to: /app/game/play
     */
    @MessageMapping("/game/play")
    public void playCard(PlayCardRequest request) {
        try {
            gameEngine.playCard(request.gameId(), request.playerId(), request.card());
            
            GameState game = gameEngine.getGame(request.gameId())
                    .orElseThrow(() -> new IllegalArgumentException("Game not found"));
            
            // Check if table was cleared
            boolean tableCleared = game.getTablePile().isEmpty();
            
            GameStateUpdate update = new GameStateUpdate(
                    game,
                    "Card played",
                    tableCleared ? GameStateUpdate.UpdateType.TABLE_CLEARED : GameStateUpdate.UpdateType.CARD_PLAYED
            );
            
            // Broadcast to all clients in this game
            messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), update);
            
            // Check if it's a bot's turn next
            aiBotService.checkAndMakeBotMove(request.gameId());
            
        } catch (Exception e) {
            log.error("Error playing card: {}", e.getMessage());
            GameStateUpdate errorUpdate = new GameStateUpdate(
                    null,
                    e.getMessage(),
                    GameStateUpdate.UpdateType.ERROR
            );
            messagingTemplate.convertAndSendToUser(
                    request.playerId(),
                    "/queue/errors",
                    errorUpdate
            );
        }
    }
    
    /**
     * Handles draw card action from client.
     * Client sends to: /app/game/draw
     */
    @MessageMapping("/game/draw")
    public void drawCard(DrawCardRequest request) {
        try {
            gameEngine.drawCard(request.gameId(), request.playerId());
            
            GameState game = gameEngine.getGame(request.gameId())
                    .orElseThrow(() -> new IllegalArgumentException("Game not found"));
            GameStateUpdate update = new GameStateUpdate(
                    game,
                    "Card drawn",
                    GameStateUpdate.UpdateType.CARD_DRAWN
            );
            
            // Broadcast to all clients in this game
            messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), update);
            
            // Check if it's a bot's turn next
            aiBotService.checkAndMakeBotMove(request.gameId());
            
        } catch (Exception e) {
            log.error("Error drawing card: {}", e.getMessage());
            GameStateUpdate errorUpdate = new GameStateUpdate(
                    null,
                    e.getMessage(),
                    GameStateUpdate.UpdateType.ERROR
            );
            messagingTemplate.convertAndSendToUser(
                    request.playerId(),
                    "/queue/errors",
                    errorUpdate
            );
        }
    }


    /**
     * Handles collect table action from client.
     * Client sends to: /app/game/collect
     */
    @MessageMapping("/game/collect")
    public void collectTable(DrawCardRequest request) {
        try {
            gameEngine.collectTable(request.gameId(), request.playerId());
            
            GameState game = gameEngine.getGame(request.gameId())
                    .orElseThrow(() -> new IllegalArgumentException("Game not found"));
            GameStateUpdate update = new GameStateUpdate(
                    game,
                    "Table collected",
                    GameStateUpdate.UpdateType.TABLE_COLLECTED
            );
            
            // Broadcast to all clients in this game
            messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), update);
            
            // Check if it's a bot's turn next
            aiBotService.checkAndMakeBotMove(request.gameId());
            
        } catch (Exception e) {
            log.error("Error collecting table: {}", e.getMessage());
            GameStateUpdate errorUpdate = new GameStateUpdate(
                    null,
                    e.getMessage(),
                    GameStateUpdate.UpdateType.ERROR
            );
            messagingTemplate.convertAndSendToUser(
                    request.playerId(),
                    "/queue/errors",
                    errorUpdate
            );
        }
    }
}
