package gtc.dcc.put0.core.engine;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.Deck;
import gtc.dcc.put0.core.model.Player;

public class GameContext {
    private final Deck initialDeck;
    private final List<Player> players;
    private final List<Card> tableCards;
    private int currentPlayerIndex;

    public GameContext(List<Player> players) {
        this.initialDeck = new Deck();
        this.players = new ArrayList<>(players);
        this.tableCards = new ArrayList<>();
        this.currentPlayerIndex = 0;
    }

    public Deck getInitialDeck() {
        return initialDeck;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty())
            return null;
        return players.get(currentPlayerIndex);
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public List<Card> getTableCards() {
        return tableCards;
    }

    public void addCardToTable(Card card) {
        tableCards.add(card);
    }

    public void clearTable() {
        tableCards.clear();
    }

    @Override
    public String toString() {
        return "GameContext{players=" + players.size() + ", table=" + tableCards.size() + "}";
    }
}
