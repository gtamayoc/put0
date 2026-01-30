package com.game.core.bot;

import com.game.core.engine.GameEngine;
import com.game.core.model.GameState;
import com.game.core.model.Player;

/**
 * Interface for Bot Logic.
 * Decides and executes the bot's turn.
 */
public interface BotStrategy {
    /**
     * Executes the bot's turn.
     * @param game The current game state.
     * @param engine The engine to execute actions (play, draw, collect).
     * @param botPlayer The bot player instance.
     */
    void playTurn(GameState game, GameEngine engine, Player botPlayer);
}
