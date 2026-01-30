package com.game.server.put0.dto;

import com.game.core.model.GameState;
import com.game.core.model.MatchMode;

/**
 * DTO for room creation response.
 */
public class RoomResponse {
    private String gameId;
    private String playerId;
    private GameState gameState;
    private String message;
    private MatchMode mode;

    public RoomResponse() {}

    public RoomResponse(String gameId, String playerId, GameState gameState, String message, MatchMode mode) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.gameState = gameState;
        this.message = message;
        this.mode = mode;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MatchMode getMode() { return mode; }
    public void setMode(MatchMode mode) { this.mode = mode; }
}
