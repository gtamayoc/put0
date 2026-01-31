package com.game.server.put0.dto;

/**
 * DTO for draw card requests from clients.
 */
public class DrawCardRequest {
    private String gameId;
    private String playerId;

    public DrawCardRequest() {}

    public DrawCardRequest(String gameId, String playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
}
