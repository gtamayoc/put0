package com.game.server.put0.dto;

import com.game.server.put0.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for game state updates sent to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateUpdate {
    private GameState gameState;
    private String message;
    private UpdateType type;
    
    public enum UpdateType {
        CARD_PLAYED,
        CARD_DRAWN,
        TABLE_CLEARED,
        TURN_CHANGED,
        GAME_STARTED,
        GAME_FINISHED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        ERROR
    }
}
