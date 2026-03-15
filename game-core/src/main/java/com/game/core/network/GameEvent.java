package com.game.core.network;

import com.game.core.model.Card;
import com.game.core.model.GameStatus;

public sealed interface GameEvent permits 
        GameEvent.CardPlayedEvent, 
        GameEvent.CardDrawnEvent, 
        GameEvent.TableClearedEvent,
        GameEvent.TurnChangedEvent,
        GameEvent.GameStartedEvent,
        GameEvent.GameFinishedEvent,
        GameEvent.PlayerJoinedEvent,
        GameEvent.PlayerLeftEvent,
        GameEvent.TableCollectedEvent,
        GameEvent.ErrorEvent {

    String gameId();

    static record CardPlayedEvent(
            String gameId,
            String playerId,
            Card card,
            Card previousTableCard
    ) implements GameEvent {}

    static record CardDrawnEvent(
            String gameId,
            String playerId,
            Card drawnCard
    ) implements GameEvent {}

    static record TableClearedEvent(
            String gameId,
            String playerId,
            Card clearingCard
    ) implements GameEvent {}

    static record TurnChangedEvent(
            String gameId,
            String playerId,
            int newPlayerIndex
    ) implements GameEvent {}

    static record GameStartedEvent(
            String gameId,
            GameStatus status
    ) implements GameEvent {}

    static record GameFinishedEvent(
            String gameId,
            String winnerId,
            int winnerScore
    ) implements GameEvent {}

    static record PlayerJoinedEvent(
            String gameId,
            String playerId,
            String playerName,
            boolean isBot
    ) implements GameEvent {}

    static record PlayerLeftEvent(
            String gameId,
            String playerId
    ) implements GameEvent {}

    static record TableCollectedEvent(
            String gameId,
            String playerId,
            int cardCount
    ) implements GameEvent {}

    static record ErrorEvent(
            String gameId,
            String playerId,
            String errorMessage
    ) implements GameEvent {}
}
