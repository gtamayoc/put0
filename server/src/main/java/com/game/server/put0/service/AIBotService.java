package com.game.server.put0.service;

import com.game.core.bot.BotStrategy;
import com.game.core.bot.DefaultBotStrategy;
import com.game.core.model.GameState;
import com.game.core.model.Player;
import com.game.core.model.GameStatus;
import com.game.server.put0.dto.GameStateUpdate;
import com.game.server.put0.exception.BotNotFoundException;
import com.game.server.put0.exception.GameNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AIBotService {

    private static final long BOT_MOVE_DELAY_MS = 700L;

    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;
    private final BotStrategy botStrategy;
    private final ScheduledExecutorService scheduler;

    public AIBotService(GameEngine gameEngine, SimpMessagingTemplate messagingTemplate) {
        this.gameEngine = gameEngine;
        this.messagingTemplate = messagingTemplate;
        this.botStrategy = new DefaultBotStrategy();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void makeMove(String gameId, String botPlayerId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }

        Player bot = game.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .filter(Player::isBot)
                .findFirst()
                .orElseThrow(() -> new BotNotFoundException(botPlayerId));

        try {
            botStrategy.playTurn(game, gameEngine.getCore(), bot);
        } catch (Exception e) {
            log.error("Bot strategy failed", e);
            return;
        }

        String lastAction = game.getLastAction();
        GameStateUpdate.UpdateType type = determineUpdateType(lastAction);

        broadcastUpdate(gameId, lastAction != null ? lastAction : "Bot moved", type);

        checkAndMakeBotMove(gameId);
    }

    private GameStateUpdate.UpdateType determineUpdateType(String lastAction) {
        if (lastAction == null) {
            return GameStateUpdate.UpdateType.CARD_PLAYED;
        }
        if (lastAction.contains("collected")) {
            return GameStateUpdate.UpdateType.TABLE_COLLECTED;
        }
        if (lastAction.contains("DREW")) {
            return GameStateUpdate.UpdateType.CARD_DRAWN;
        }
        return GameStateUpdate.UpdateType.CARD_PLAYED;
    }

    @Async("gameExecutor")
    public void checkAndMakeBotMove(String gameId) {
        scheduler.schedule(() -> {
            try {
                GameState game = gameEngine.getGame(gameId);
                if (game == null) {
                    return;
                }

                Player currentPlayer = game.getCurrentPlayer();
                if (currentPlayer != null && currentPlayer.isBot() && game.getStatus() == GameStatus.PLAYING) {
                    makeMove(gameId, currentPlayer.getId());
                }
            } catch (Exception e) {
                log.error("Error in bot move scheduling", e);
            }
        }, BOT_MOVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void broadcastUpdate(String gameId, String message, GameStateUpdate.UpdateType type) {
        GameState game = gameEngine.getGame(gameId);
        if (game != null) {
            GameStateUpdate update = new GameStateUpdate(game, message, type);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
        }
    }
}
