package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import gtc.dcc.put0.core.model.Card;

public class GameState implements Serializable {
    @SerializedName("gameId")
    private String gameId;

    @SerializedName("players")
    private List<Player> players = new ArrayList<>();

    @SerializedName("deck")
    private List<Card> deck = new ArrayList<>();

    @SerializedName("table")
    private List<Card> table = new ArrayList<>();

    @SerializedName("currentPlayerIndex")
    private int currentPlayerIndex;

    @SerializedName("status")
    private GameStatus status;

    @SerializedName("winnerId")
    private String winnerId;

    // Getters
    public String getGameId() {
        return gameId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public List<Card> getTable() {
        return table;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size())
            return null;
        return players.get(currentPlayerIndex);
    }

    public String getCurrentPlayerId() {
        Player current = getCurrentPlayer();
        return current != null ? current.getId() : null;
    }

    public Card getTopCard() {
        if (table == null || table.isEmpty())
            return null;
        return table.get(table.size() - 1);
    }
}
