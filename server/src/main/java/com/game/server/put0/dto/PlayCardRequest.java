package com.game.server.put0.dto;

import com.game.core.model.Card;

/**
 * DTO for play card requests from clients.
 */
public class PlayCardRequest {
    private String gameId;
    private String playerId;
    private Card card;

    public PlayCardRequest() {}

    public PlayCardRequest(String gameId, String playerId, Card card) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.card = card;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
}
