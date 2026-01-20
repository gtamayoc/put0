package com.game.server.put0.dto;

import com.game.server.put0.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for room creation/join responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private String gameId;
    private String playerId;
    private GameState gameState;
    private String message;
    private com.game.server.put0.model.MatchMode mode;
}
