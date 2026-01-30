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

    @SerializedName("lastAction")
    private String lastAction;

    // Setters
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void setMainDeck(List<Card> mainDeck) {
        this.mainDeck = mainDeck;
    }

    public void setTablePile(List<Card> tablePile) {
        this.tablePile = tablePile;
    }

    public void setDiscardPile(List<Card> discardPile) {
        this.discardPile = discardPile;
    }

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

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

    public String getLastAction() {
        return lastAction;
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
