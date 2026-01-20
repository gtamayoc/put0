package com.game.server.put0.dto;

import com.game.server.put0.model.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for play card requests from clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayCardRequest {
    private String gameId;
    private String playerId;
    private Card card;
}
