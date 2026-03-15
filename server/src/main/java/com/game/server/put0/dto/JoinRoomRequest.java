package com.game.server.put0.dto;

public record JoinRoomRequest(
        String gameId,
        String playerName
) {}
