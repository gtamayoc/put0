package com.game.server.put0.dto;

/**
 * DTO for joining a room.
 */
public class JoinRoomRequest {
    private String gameId;
    private String playerName;

    public JoinRoomRequest() {}

    public JoinRoomRequest(String gameId, String playerName) {
        this.gameId = gameId;
        this.playerName = playerName;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
}
