package com.game.server.put0.dto;

import com.game.core.model.MatchMode;

/**
 * DTO for creating a new game room.
 */
public class CreateRoomRequest {
    private String playerName;
    private Boolean isPrivate;
    private Integer maxPlayers = 4;
    private Integer botCount = 0; // Number of AI bots to add
    private MatchMode mode;

    public CreateRoomRequest() {}

    public CreateRoomRequest(String playerName, Boolean isPrivate, Integer maxPlayers, Integer botCount, MatchMode mode) {
        this.playerName = playerName;
        this.isPrivate = isPrivate;
        this.maxPlayers = maxPlayers;
        this.botCount = botCount;
        this.mode = mode;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

    public Integer getBotCount() { return botCount; }
    public void setBotCount(Integer botCount) { this.botCount = botCount; }

    public MatchMode getMode() { return mode; }
    public void setMode(MatchMode mode) { this.mode = mode; }
}
