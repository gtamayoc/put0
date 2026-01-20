package com.game.server.put0.service;

import com.game.server.put0.dto.GameStateUpdate;
import com.game.server.put0.model.GameState;
import com.game.server.put0.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * AI Bot controller for PUT0 game.
 * Implements simple but effective AI strategy for playing cards.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIBotService {
    
    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Makes a move for an AI bot player.
     * Strategy:
     * 1. If has a 10, play it (clears table and gets another turn)
     * 2. Otherwise, play the lowest valid card (conserve high cards)
     * 3. If no valid cards, draw from deck
     */
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
        
        Card topCard = game.getTopCard();
        List<Card> playableCards = bot.getPlayableCards(topCard);
        
        if (playableCards.isEmpty()) {
            // No playable cards - draw from deck
            try {
                gameEngine.drawCard(gameId, botPlayerId);
                log.info("Bot {} drew a card", bot.getName());
                broadcastUpdate(gameId, "Bot " + bot.getName() + " drew a card", GameStateUpdate.UpdateType.CARD_DRAWN);
            } catch (Exception e) {
                log.error("Bot {} failed to draw card: {}", bot.getName(), e.getMessage());
            }
            return;
        }
        
        // Strategy: Prefer 10s (clear table), otherwise play lowest card
        Card cardToPlay = playableCards.stream()
                .filter(Card::clearsTable)
                .findFirst()
                .orElse(playableCards.stream()
                        .min(Comparator.comparingInt(Card::getValue))
                        .orElse(playableCards.get(0)));
        
        try {
            gameEngine.playCard(gameId, botPlayerId, cardToPlay);
            log.info("Bot {} played {}", bot.getName(), cardToPlay);
            
            // Broadcast bot move
            broadcastUpdate(gameId, "Bot " + bot.getName() + " played " + cardToPlay, GameStateUpdate.UpdateType.CARD_PLAYED);
            
            // If bot cleared the table or won, it gets another turn
            if (cardToPlay.clearsTable() && !bot.hasWon()) {
                // Recursively make another move
                makeMove(gameId, botPlayerId);
            }
        } catch (Exception e) {
            log.error("Bot {} failed to play card: {}", bot.getName(), e.getMessage());
        }
    }
    
    /**
     * Checks if it's a bot's turn and makes a move automatically.
     */
    public void checkAndMakeBotMove(String gameId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) {
            return;
        }
        
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isBot()) {
            // Add a small delay to make it feel more natural
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
