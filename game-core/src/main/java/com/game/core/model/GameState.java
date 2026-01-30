package com.game.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the current state of a PUT0 game.
 */
@Data
@NoArgsConstructor
public class GameState {
    
    private String gameId;
    private List<Player> players = new ArrayList<>();
    
    // Pile 1: Main Deck (Drawing source)
    private List<Card> mainDeck = new ArrayList<>();
    
    // Pile 2: Table Pile (Active play area)
    private List<Card> tablePile = new ArrayList<>();
    
    // Pile 3: Discard Pile (Burned cards)
    private List<Card> discardPile = new ArrayList<>();
    
    private int currentPlayerIndex = 0;
    private GameStatus status = GameStatus.WAITING;
    private String winnerId;
    private MatchMode mode;
    private String lastAction;
    
    public GameState(String gameId) {
        this.gameId = gameId;
    }
    
    /**
     * Gets the current player whose turn it is.
     */
    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }
    
    /**
     * Gets the top card on the table (most recently played).
     */
    public Card getTopCard() {
        if (tablePile.isEmpty()) {
            return null;
        }
        return tablePile.get(tablePile.size() - 1);
    }
    
    /**
     * Advances to the next player's turn.
     */
    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }
    
    /**
     * Clears the table (when a 10 is played or 4 cards of same value).
     * Moves cards to discardPile ("Quemadas").
     */
    public void clearTable() {
        if (!tablePile.isEmpty()) {
            discardPile.addAll(tablePile);
            tablePile.clear();
        }
    }
    
    /**
     * Checks if the last 4 cards on the table have the same value.
     * This triggers a table clear.
     */
    public boolean shouldClearTable() {
        if (tablePile.size() < 4) {
            return false;
        }
        
        int lastValue = tablePile.get(tablePile.size() - 1).getValue();
        for (int i = tablePile.size() - 4; i < tablePile.size(); i++) {
            if (tablePile.get(i).getValue() != lastValue) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Adds a player to the game.
     */
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    /**
     * Player tracks all table cards into their hand.
     * Used when a player cannot play (penalty) or during Hidden phase bad guess.
     */
    public void collectTable(Player player) {
        if (!tablePile.isEmpty()) {
            player.getHand().addAll(tablePile);
            tablePile.clear();
        }
    }

    /**
     * Checks if the game is ready to start.
     */
    public boolean canStart() {
        return players.size() >= 2 && status == GameStatus.WAITING;
    }
}
