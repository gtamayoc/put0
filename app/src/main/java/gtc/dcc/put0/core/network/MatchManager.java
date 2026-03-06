package gtc.dcc.put0.core.network;

import androidx.lifecycle.LiveData;
import java.util.List;

import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.model.Card;

/**
 * Interface to unify gameplay across different network solutions.
 * Implementations: Online (HTTP/WebSocket), Offline (Bluetooth Host/Client),
 * Local (CPU Bot).
 */
public interface MatchManager {
    LiveData<GameState> getGameState();

    LiveData<String> getError();

    LiveData<String> getCurrentGameId();

    LiveData<String> getCurrentPlayerId();

    void playCard(String playerId, Card card);

    void playCards(String playerId, List<Card> cards);

    void drawCard(String playerId);

    void collectTable(String playerId);

    void leaveGame();
}
