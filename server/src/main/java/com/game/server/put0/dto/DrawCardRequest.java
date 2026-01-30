package com.game.server.put0.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for draw card requests from clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawCardRequest {
    private String gameId;
    private String playerId;
}
