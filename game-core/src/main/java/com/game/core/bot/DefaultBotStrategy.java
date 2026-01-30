package com.game.core.bot;

import com.game.core.engine.GameEngine;
import com.game.core.model.Card;
import com.game.core.model.GameState;
import com.game.core.model.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

/**
 * Default implementation of Bot Strategy.
 * Logic:
 * 1. Play 10 (clear table) if available.
 * 2. Play lowest power valid card.
 * 3. Draw or Collect if no valid moves.
 * 4. Random choice during Blind Play (Phase 4).
 */
@Slf4j
public class DefaultBotStrategy implements BotStrategy {

    private final Random random = new Random();

    @Override
    public void playTurn(GameState game, GameEngine engine, Player botPlayer) {
        if (game == null || botPlayer == null) return;
        
        log.info("[BOT-THINK] Bot {} is thinking...", botPlayer.getName());

        Card topCard = game.getTopCard();
        List<Card> playableCards = botPlayer.getPlayableCards(topCard);
        
        if (playableCards.isEmpty()) {
            // No playable cards - check if table has cards to "eat" (collect)
            try {
                if (!game.getTablePile().isEmpty()) {
                    engine.collectTable(game, botPlayer.getId());
                    log.info("[BOT-ACTION] Bot {} COLLECTED the table (No playable cards).", botPlayer.getName());
                } else {
                    // Table empty - draw from deck
                    engine.drawCard(game, botPlayer.getId());
                    log.info("[BOT-ACTION] Bot {} DREW a card (No playable cards, Table empty).", botPlayer.getName());
                }
            } catch (Exception e) {
                log.error("[BOT-ERROR] Bot {} failed to move (collect/draw): {}", botPlayer.getName(), e.getMessage());
            }
            return;
        }
        
        // Strategy Logic
        boolean isPhase4 = playableCards.stream().allMatch(Card::isHidden);
        Card cardToPlay;
        
        if (isPhase4) {
            // Blind play: Pick random
            cardToPlay = playableCards.get(random.nextInt(playableCards.size()));
        } else {
            // Check for clearing cards (10s)
            List<Card> clearingCards = playableCards.stream()
                .filter(Card::clearsTable)
                .toList();
            
            if (!clearingCards.isEmpty()) {
                // Pick a random clearing card if multiple exist
                cardToPlay = clearingCards.get(random.nextInt(clearingCards.size()));
            } else {
                // Otherwise, find the lowest POWER cards and pick one randomly
                int minPower = playableCards.stream()
                        .mapToInt(Card::getPower)
                        .min()
                        .orElse(0);
                
                List<Card> lowestPowerCards = playableCards.stream()
                        .filter(c -> c.getPower() == minPower)
                        .toList();
                
                cardToPlay = lowestPowerCards.get(random.nextInt(lowestPowerCards.size()));
            }
        }
        
        try {
            engine.playCard(game, botPlayer.getId(), cardToPlay);
            log.info("[BOT-ACTION] Bot {} PLAYED {} (Value: {}).", botPlayer.getName(), cardToPlay, cardToPlay.getValue());
        } catch (Exception e) {
            log.error("[BOT-ERROR] Bot {} failed to play card: {}", botPlayer.getName(), e.getMessage());
        }
    }
}
