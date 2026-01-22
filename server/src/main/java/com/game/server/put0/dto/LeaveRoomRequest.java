package com.game.server.put0.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for leaving a game room.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRoomRequest {
    private String gameId;
    private String playerId;
}
