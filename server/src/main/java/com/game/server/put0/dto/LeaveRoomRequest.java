package com.game.server.put0.dto;

public record LeaveRoomRequest(
        String gameId,
        String playerId
) {}
