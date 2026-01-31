package com.game.server.put0.dto;

import com.game.core.model.GameState;

/**
 * DTO for game state updates sent to clients.
 */
public class GameStateUpdate {
    private GameState gameState;
    private String message;
    private UpdateType type;
    
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

    public GameStateUpdate() {}

    public GameStateUpdate(GameState gameState, String message, UpdateType type) {
        this.gameState = gameState;
        this.message = message;
        this.type = type;
    }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UpdateType getType() { return type; }
    public void setType(UpdateType type) { this.type = type; }
}
