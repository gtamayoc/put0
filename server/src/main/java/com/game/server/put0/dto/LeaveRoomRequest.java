package com.game.server.put0.dto;

/**
 * DTO for leaving a room.
 */
public class LeaveRoomRequest {
    private String gameId;
    private String playerId;

    public LeaveRoomRequest() {}

    public LeaveRoomRequest(String gameId, String playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
}
