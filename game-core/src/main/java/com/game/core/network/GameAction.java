package com.game.core.network;

/**
 * Defines the possible actions a player can take that will be synced across the
 * network/Bluetooth.
 */
public enum GameAction {
    PLAY_CARD,
    DRAW_CARD,
    COLLECT_TABLE,
    PASS,
    RESTART_GAME,
    SET_PAUSED,
    PROMOTE_TO_HOST
}
