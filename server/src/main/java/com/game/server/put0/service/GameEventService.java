package com.game.server.put0.service;

import com.game.core.network.GameEvent;
import com.game.core.network.GameEvent.*;
import com.game.server.put0.dto.GameStateUpdate;
import com.game.server.put0.dto.GameStateUpdate.UpdateType;
import com.game.core.model.Card;
import com.game.core.model.GameState;
import com.game.core.model.GameStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class GameEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public GameEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishEvent(String gameId, GameEvent event) {
        handleEvent(gameId, event);
    }

    private void handleEvent(String gameId, GameEvent event) {
        if (event instanceof CardPlayedEvent e) {
            handleCardPlayed(gameId, e);
        } else if (event instanceof CardDrawnEvent e) {
            handleCardDrawn(gameId, e);
        } else if (event instanceof TableClearedEvent e) {
            handleTableCleared(gameId, e);
        } else if (event instanceof TurnChangedEvent e) {
            handleTurnChanged(gameId, e);
        } else if (event instanceof GameStartedEvent e) {
            handleGameStarted(gameId, e);
        } else if (event instanceof GameFinishedEvent e) {
            handleGameFinished(gameId, e);
        } else if (event instanceof PlayerJoinedEvent e) {
            handlePlayerJoined(gameId, e);
        } else if (event instanceof PlayerLeftEvent e) {
            handlePlayerLeft(gameId, e);
        } else if (event instanceof TableCollectedEvent e) {
            handleTableCollected(gameId, e);
        } else if (event instanceof ErrorEvent e) {
            handleError(gameId, e);
        }
    }

    private void handleCardPlayed(String gameId, CardPlayedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Card played by " + event.playerId(),
                UpdateType.CARD_PLAYED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleCardDrawn(String gameId, CardDrawnEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Card drawn by " + event.playerId(),
                UpdateType.CARD_DRAWN
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleTableCleared(String gameId, TableClearedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Table cleared by " + event.playerId(),
                UpdateType.TABLE_CLEARED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleTurnChanged(String gameId, TurnChangedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Turn changed to player at index " + event.newPlayerIndex(),
                UpdateType.TURN_CHANGED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleGameStarted(String gameId, GameStartedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Game started",
                UpdateType.GAME_STARTED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleGameFinished(String gameId, GameFinishedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Game finished! Winner: " + event.winnerId(),
                UpdateType.GAME_FINISHED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handlePlayerJoined(String gameId, PlayerJoinedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                event.playerName() + " joined the game",
                UpdateType.PLAYER_JOINED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handlePlayerLeft(String gameId, PlayerLeftEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Player left the game",
                UpdateType.PLAYER_LEFT
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleTableCollected(String gameId, TableCollectedEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                "Table collected by " + event.playerId(),
                UpdateType.TABLE_COLLECTED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
    }

    private void handleError(String gameId, ErrorEvent event) {
        GameStateUpdate update = new GameStateUpdate(
                null,
                event.errorMessage(),
                UpdateType.ERROR
        );
        messagingTemplate.convertAndSendToUser(
                event.playerId(),
                "/queue/errors",
                update
        );
    }
}
