package com.game.core.model;

/**
 * Represents the status of a game.
 */
public enum GameStatus {
    WAITING, // Waiting for players to join
    PLAYING, // Game in progress
    FINISHED, // Game completed
    PAUSED, // Game paused due to player disconnection
    WAITING_FOR_RECONNECTION // Waiting for a disconnected player to rejoin
}
