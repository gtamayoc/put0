package com.game.server.put0.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new game room.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private String playerName;
    private Boolean isPrivate;
    private Integer maxPlayers = 4;
    private Integer botCount = 0; // Number of AI bots to add
    private com.game.server.put0.model.MatchMode mode;
}
