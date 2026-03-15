package com.game.server.put0.exception;

public class BotNotFoundException extends RuntimeException {
    public BotNotFoundException(String playerId) {
        super("Bot player not found or not a bot: " + playerId);
    }
}
