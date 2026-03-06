package com.game.core.network;

import java.util.List;

/**
 * Data Transfer Object for sending player actions across network/Bluetooth
 * connections.
 * This class is designed to be serialized with Gson.
 */
public class GameEventDTO {
    private GameAction action;
    private String playerId;

    // Optional payload (e.g., list of card IDs played ["H7", "S7"])
    private List<String> payload;

    public GameEventDTO() {
        // Default constructor required for Gson deserialization
    }

    public GameEventDTO(GameAction action, String playerId, List<String> payload) {
        this.action = action;
        this.playerId = playerId;
        this.payload = payload;
    }

    public GameAction getAction() {
        return action;
    }

    public void setAction(GameAction action) {
        this.action = action;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public List<String> getPayload() {
        return payload;
    }

    public void setPayload(List<String> payload) {
        this.payload = payload;
    }
}
