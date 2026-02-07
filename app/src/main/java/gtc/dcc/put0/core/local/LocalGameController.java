package gtc.dcc.put0.core.local;

import android.os.Handler;
import android.os.Looper;

import java.util.UUID;

import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.data.model.MatchMode;
import gtc.dcc.put0.core.data.model.Player;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.utils.CoreLogger;
import com.game.core.engine.GameEngine;
import com.game.core.bot.BotStrategy;
import com.game.core.bot.DefaultBotStrategy;

/**
 * Controller for offline Solo vs Bot games.
 * Wraps the game-core engine and handles turn flow locally.
 */
public class LocalGameController {

    public static final String HUMAN_ID = "local-human";
    private final GameEngine coreEngine;
    private final BotStrategy botStrategy;
    private com.game.core.model.GameState coreState;
    private final OnGameStateChangeListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface OnGameStateChangeListener {
        void onGameStateChanged(GameState newState);
    }

    public LocalGameController(OnGameStateChangeListener listener) {
        this.coreEngine = new GameEngine();
        this.botStrategy = new DefaultBotStrategy();
        this.listener = listener;
    }

    public void startSoloGame(String playerName, int botCount, int deckSize) {
        String gameId = "local-" + UUID.randomUUID().toString();
        coreState = coreEngine.createGame(gameId);
        coreState.setDeckSize(deckSize);

        // Add human player
        com.game.core.model.Player human = new com.game.core.model.Player(
                HUMAN_ID, playerName, false);
        coreEngine.addPlayer(coreState, human);

        // Add bots
        for (int i = 0; i < botCount; i++) {
            com.game.core.model.Player bot = new com.game.core.model.Player(
                    UUID.randomUUID().toString(), "Bot " + (i + 1), true);
            coreEngine.addPlayer(coreState, bot);
        }

        coreEngine.startGame(coreState);
        notifyListener();

        // Check if first turn is bot (rare but possible)
        checkBotTurn();
    }

    public void playCard(String playerId, Card androidCard) {
        if (coreState == null)
            return;

        try {
            com.game.core.model.Card coreCard = GameMapper.toCoreCard(androidCard);
            coreEngine.playCard(coreState, playerId, coreCard);
            notifyListener();

            // Trigger bot turn after human move
            checkBotTurn();
        } catch (Exception e) {
            // Log local error
            CoreLogger.e("LOCAL_GAME", "Error playing card: " + e.getMessage());
            listener.onGameStateChanged(GameMapper.toAndroidState(coreState)); // Refresh UI
        }
    }

    public void drawCard(String playerId) {
        if (coreState == null)
            return;
        try {
            coreEngine.drawCard(coreState, playerId);
            notifyListener();
            checkBotTurn();
        } catch (Exception e) {
            // Log and notify error
            CoreLogger.e("LOCAL_GAME", "Error drawing card: " + e.getMessage());
            listener.onGameStateChanged(GameMapper.toAndroidState(coreState)); // Refresh UI
        }
    }

    public void collectTable(String playerId) {
        if (coreState == null)
            return;
        try {
            coreEngine.collectTable(coreState, playerId);
            notifyListener();
            checkBotTurn();
        } catch (Exception e) {
            // Log and notify error
            CoreLogger.e("LOCAL_GAME", "Error collecting table: " + e.getMessage());
            listener.onGameStateChanged(GameMapper.toAndroidState(coreState)); // Refresh UI
        }
    }

    private void checkBotTurn() {
        if (coreState == null || coreState.getStatus() != com.game.core.model.GameStatus.PLAYING)
            return;

        com.game.core.model.Player currentPlayer = coreState.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isBot()) {
            // Artificial delay for UX
            handler.postDelayed(() -> {
                if (coreState == null)
                    return;
                botStrategy.playTurn(coreState, coreEngine, currentPlayer);
                notifyListener();
                // Check if next turn is also a bot
                checkBotTurn();
            }, 1000);
        }
    }

    private void notifyListener() {
        if (listener != null) {
            GameState androidState = GameMapper.toAndroidState(coreState);
            handler.post(() -> listener.onGameStateChanged(androidState));
        }
    }
}
