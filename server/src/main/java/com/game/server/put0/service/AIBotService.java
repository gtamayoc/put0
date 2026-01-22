package com.game.server.put0.service;

import com.game.server.put0.dto.GameStateUpdate;
import com.game.server.put0.model.Card;
import com.game.server.put0.model.GameState;
import com.game.server.put0.model.Player;
import com.game.server.put0.model.GameStatus;
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
            // No playable cards - check if table has cards to "eat" (collect)
            try {
                if (!game.getTablePile().isEmpty()) {
                    gameEngine.collectTable(gameId, botPlayerId);
                    log.info("[BOT-ACTION] Bot {} COLLECTED the table (No playable cards).", bot.getName());
                    broadcastUpdate(gameId, "Bot " + bot.getName() + " collected the table", GameStateUpdate.UpdateType.TABLE_COLLECTED);
                } else {
                    // Table empty - draw from deck
                    gameEngine.drawCard(gameId, botPlayerId);
                    log.info("[BOT-ACTION] Bot {} DREW a card (No playable cards, Table empty).", bot.getName());
                    broadcastUpdate(gameId, "Bot " + bot.getName() + " drew a card", GameStateUpdate.UpdateType.CARD_DRAWN);
                }
                
                // Trigger next player if it's a bot
                checkAndMakeBotMove(gameId);
            } catch (Exception e) {
                log.error("Bot {} failed to move (collect/draw): {}", bot.getName(), e.getMessage());
            }
            return;
        }
        
        // Strategy: 
        // 1. If we are in Phase 4 (all playable cards are hidden), pick randomly (Blind Play)
        // 2. Otherwise, prefer 10s (clear table), or play lowest card
        boolean isPhase4 = playableCards.stream().allMatch(Card::isHidden);
        Card cardToPlay;
        
        if (isPhase4) {
            cardToPlay = playableCards.get(new java.util.Random().nextInt(playableCards.size()));
        } else {
            // Check for clearing cards (10s)
            List<Card> clearingCards = playableCards.stream()
                .filter(Card::clearsTable)
                .toList();
            
            if (!clearingCards.isEmpty()) {
                // Pick a random clearing card if multiple exist
                cardToPlay = clearingCards.get(new java.util.Random().nextInt(clearingCards.size()));
            } else {
                // Otherwise, find the lowest POWER cards and pick one randomly
                int minPower = playableCards.stream()
                        .mapToInt(Card::getPower)
                        .min()
                        .orElse(0);
                
                List<Card> lowestPowerCards = playableCards.stream()
                        .filter(c -> c.getPower() == minPower)
                        .toList();
                
                cardToPlay = lowestPowerCards.get(new java.util.Random().nextInt(lowestPowerCards.size()));
            }
        }
        
        try {
            gameEngine.playCard(gameId, botPlayerId, cardToPlay);
            log.info("[BOT-ACTION] Bot {} PLAYED {} (Value: {}).", bot.getName(), cardToPlay, cardToPlay.getValue());
            
            // Broadcast bot move
            broadcastUpdate(gameId, "Bot " + bot.getName() + " played " + cardToPlay, GameStateUpdate.UpdateType.CARD_PLAYED);
            
            // Check if it's still the bot's turn (e.g. cleared table) or if it's another bot's turn next
            Player nextPlayer = game.getCurrentPlayer();
            if (nextPlayer != null && nextPlayer.isBot() && 
                !bot.hasWon() && game.getStatus() == GameStatus.PLAYING) {
                
                if (nextPlayer.getId().equals(botPlayerId)) {
                    log.info("[BOT-TURN] Bot {} taking another turn.", bot.getName());
                    makeMove(gameId, botPlayerId);
                } else {
                    // It's a different bot's turn
                    checkAndMakeBotMove(gameId);
                }
            }
        } catch (Exception e) {
            log.error("[BOT-ERROR] Bot {} failed to play card: {}", bot.getName(), e.getMessage());
        }
    }
    
    /**
     * Checks if it's a bot's turn and makes a move automatically.
     * Async execution to prevent blocking the WebSocket/Controller thread.
     */
    @org.springframework.scheduling.annotation.Async("gameExecutor")
    public void checkAndMakeBotMove(String gameId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) {
            return;
        }
        
        // Synchronize on the game object to ensure we read consistent state
        // Note: We need to handle this carefully. For now, reading is okay, 
        // but makeMove will need the lock when writing.
        
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isBot()) {
            // Add a small delay to make it feel more natural
            try {
                // Now this sleep only blocks the background thread, which is fine
                Thread.sleep(700); 
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
