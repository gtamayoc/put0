package com.game.server.put0.dto;

import com.game.core.model.GameState;

public record GameStateUpdate(
        GameState gameState,
        String message,
        UpdateType type
) {
    public enum UpdateType {
        CARD_PLAYED,
        CARD_DRAWN,
        TABLE_CLEARED,
        TURN_CHANGED,
        GAME_STARTED,
        GAME_FINISHED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        TABLE_COLLECTED,
        ERROR
    }
}
