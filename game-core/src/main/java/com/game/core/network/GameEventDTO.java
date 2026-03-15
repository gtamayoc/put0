package com.game.core.network;

import java.util.List;

public record GameEventDTO(
        GameAction action,
        String playerId,
        List<String> payload
) {}
