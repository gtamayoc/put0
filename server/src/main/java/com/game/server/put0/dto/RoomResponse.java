package com.game.server.put0.dto;

import com.game.core.model.GameState;
import com.game.core.model.MatchMode;

public record RoomResponse(
        String gameId,
        String playerId,
        GameState gameState,
        String message,
        MatchMode mode
) {}
