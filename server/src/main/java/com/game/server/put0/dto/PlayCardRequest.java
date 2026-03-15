package com.game.server.put0.dto;

import com.game.core.model.Card;

public record PlayCardRequest(
        String gameId,
        String playerId,
        Card card
) {}
