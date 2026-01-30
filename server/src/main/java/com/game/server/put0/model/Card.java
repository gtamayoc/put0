package com.game.server.put0.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a playing card in the PUT0 game.
 * Cards have values from 1 (Ace) to 13 (King).
 * Comparison logic follows game rules: A > K > Q ... > 3.
 * 2 and 10 have special effects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    
    /**
     * Card value: 1 (Ace) to 13 (King)
     */
    private int value;
    
    /**
     * Card suit: HEARTS, DIAMONDS, CLUBS, SPADES
     */
    private Suit suit;

    /**
     * Whether the card is hidden (Phase 4).
     */
    private boolean hidden = false;

    public Card(int value, Suit suit) {
        this.value = value;
        this.suit = suit;
        this.hidden = false;
    }
    
    /**
     * Checks if this card can be played on top of the given card.
     * Rule: Can only play a card with equal or higher power.
     * Exceptions:
     * - 2 can be played on anything (resets/wild)
     * - 10 can be played on anything (clears table)
     * 
     * @param tableCard The current card on the table
     * @return true if this card can be played
     */
    public boolean canPlayOn(Card tableCard) {
        if (tableCard == null) {
            return true; // Can play any card on empty table
        }
        
        // Special cards: 2 and 10 can always be played
        if (this.value == 2 || this.value == 10) {
            return true;
        }

        // If table card is 2 (and wasn't cleared yet), it acts as "reset" (low value),
        // so any card can be played on it? 
        // Rule Interpretation: "2 < 3 ...". 
        // If 2 is on table, effectively the required value starts from bottom? 
        // Usually 2 resets the count to 0. So yes, any card can follow a 2.
        if (tableCard.getValue() == 2) {
            return true;
        }
        
        // Normal rule: must be equal or higher POWER
        return this.getPower() >= tableCard.getPower();
    }
    
    /**
     * Checks if this card has the special "clear table" effect.
     * 
     * @return true if this is a 10
     */
    public boolean clearsTable() {
        return this.value == 10;
    }

    /**
     * Gets the power of the card for comparison.
     * Ace (1) -> 14
     * Others -> value
     * Note: 2 and 10 are handled specially in canPlayOn, 
     * but if compared strictly default logic applies.
     */
    public int getPower() {
        if (this.value == 1) return 14; // Ace is high
        return this.value;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        // Only compare value and suit, NOT hidden flag
        return value == card.value && suit == card.suit;
    }

    @Override
    public int hashCode() {
        // Only hash value and suit, NOT hidden flag
        return 31 * value + (suit != null ? suit.hashCode() : 0);
    }

    @Override
    public String toString() {
        return valueToString() + " of " + suit;
    }
    
    private String valueToString() {
        return switch (value) {
            case 1 -> "Ace";
            case 11 -> "Jack";
            case 12 -> "Queen";
            case 13 -> "King";
            default -> String.valueOf(value);
        };
    }
}
