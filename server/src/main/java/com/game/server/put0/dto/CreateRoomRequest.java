package com.game.server.put0.dto;

import com.game.core.model.MatchMode;

public record CreateRoomRequest(
        String playerName,
        Boolean isPrivate,
        Integer maxPlayers,
        Integer botCount,
        MatchMode mode
) {
    public CreateRoomRequest {
        if (maxPlayers == null) maxPlayers = 4;
        if (botCount == null) botCount = 0;
    }
}
