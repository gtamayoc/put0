package com.game.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the PUT0 game.
 * Can be either a human player or an AI bot.
 * Manages three piles of cards: Hand, Visible (Face-up), and Hidden
 * (Face-down).
 */
@Data
@NoArgsConstructor
public class Player {

    private String id;
    private String name;

    // Phase 1 cards (current hand)
    private List<Card> hand = new ArrayList<>();

    // Phase 2 cards (face up on table)
    private List<Card> visibleCards = new ArrayList<>();

    // Phase 3 cards (face down on table)
    private List<Card> hiddenCards = new ArrayList<>();

    private boolean isBot;
    private boolean isActive = true;

    public Player(String id, String name, boolean isBot) {
        this.id = id;
        this.name = name;
        this.isBot = isBot;
    }

    /**
     * Adds a card to the player's hand (default).
     */
    public void addCard(Card card) {
        hand.add(card);
    }

    /**
     * Removes a card from the player's available piles.
     * Checks Hand -> Visible -> Hidden.
     */
    public boolean removeCard(Card card) {
        if (hand.remove(card))
            return true;
        if (visibleCards.remove(card))
            return true;
        return hiddenCards.remove(card);
    }

    /**
     * Checks if the player has any cards left in ANY pile.
     */
    public boolean hasCards() {
        return !hand.isEmpty() || !visibleCards.isEmpty() || !hiddenCards.isEmpty();
    }

    /**
     * Gets the total number of cards the player has.
     */
    public int getCardCount() {
        return hand.size() + visibleCards.size() + hiddenCards.size();
    }

    /**
     * Checks if the player has won (no cards left anywhere).
     */
    public boolean hasWon() {
        return !hasCards();
    }

    /**
     * Finds playable cards based on the current phase.
     * Logic: Must play from Hand if not empty. Then Visible. Then Hidden.
     */
    public List<Card> getPlayableCards(Card tableCard) {
        // Phase 1: Hand (Mandatory priority)
        if (!hand.isEmpty()) {
            // Return all hand cards. The GameEngine will decide if they succeed or fail
            // (Eat Table).
            // This allows the "reveal then play to fail" flow requested by the user.
            return new ArrayList<>(hand);
        }

        // Phase 2: Visible Cards (on table)
        // ONLY reachable if hand is completely empty
        if (hand.isEmpty() && !visibleCards.isEmpty()) {
            // For table cards, we still filter to guide the player?
            // Actually, to be consistent and allow "trying" visible cards that might fail:
            return new ArrayList<>(visibleCards);
        }

        // Phase 3: Hidden Cards (on table)
        // ONLY reachable if hand AND visibleCards are completely empty
        if (hand.isEmpty() && visibleCards.isEmpty() && !hiddenCards.isEmpty()) {
            return new ArrayList<>(hiddenCards);
        }

        return new ArrayList<>();
    }
}
