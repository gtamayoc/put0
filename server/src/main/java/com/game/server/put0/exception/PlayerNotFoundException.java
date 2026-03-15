package com.game.server.put0.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
