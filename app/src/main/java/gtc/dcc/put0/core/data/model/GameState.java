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

    @SerializedName("mainDeck")
    private List<Card> mainDeck = new ArrayList<>();

    @SerializedName("tablePile")
    private List<Card> tablePile = new ArrayList<>();

    @SerializedName("discardPile")
    private List<Card> discardPile = new ArrayList<>();

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

    public List<Card> getMainDeck() {
        return mainDeck;
    }

    public List<Card> getTablePile() {
        return tablePile;
    }

    public List<Card> getDiscardPile() {
        return discardPile;
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
        if (tablePile == null || tablePile.isEmpty())
            return null;
        return tablePile.get(tablePile.size() - 1);
    }
}
