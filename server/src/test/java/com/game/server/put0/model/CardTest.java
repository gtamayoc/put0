package com.game.server.put0.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Card model.
 */
@DisplayName("Card Model Tests")
class CardTest {
    
    @Test
    @DisplayName("Should allow playing card with equal value")
    void testCanPlayEqualValue() {
        Card tableCard = new Card(5, Suit.HEARTS);
        Card playerCard = new Card(5, Suit.DIAMONDS);
        
        assertTrue(playerCard.canPlayOn(tableCard));
    }
    
    @Test
    @DisplayName("Should allow playing card with higher value")
    void testCanPlayHigherValue() {
        Card tableCard = new Card(5, Suit.HEARTS);
        Card playerCard = new Card(7, Suit.DIAMONDS);
        
        assertTrue(playerCard.canPlayOn(tableCard));
    }
    
    @Test
    @DisplayName("Should not allow playing card with lower value")
    void testCannotPlayLowerValue() {
        Card tableCard = new Card(7, Suit.HEARTS);
        Card playerCard = new Card(5, Suit.DIAMONDS);
        
        assertFalse(playerCard.canPlayOn(tableCard));
    }
    
    @Test
    @DisplayName("Should allow playing 10 on any card")
    void testTenCanPlayOnAnyCard() {
        Card tableCard = new Card(13, Suit.HEARTS); // King
        Card ten = new Card(10, Suit.DIAMONDS);
        
        assertTrue(ten.canPlayOn(tableCard));
    }
    
    @Test
    @DisplayName("Should allow playing any card on empty table")
    void testCanPlayOnEmptyTable() {
        Card playerCard = new Card(5, Suit.HEARTS);
        
        assertTrue(playerCard.canPlayOn(null));
    }
    
    @Test
    @DisplayName("Should identify 10 as table-clearing card")
    void testTenClearsTable() {
        Card ten = new Card(10, Suit.HEARTS);
        Card notTen = new Card(9, Suit.HEARTS);
        
        assertTrue(ten.clearsTable());
        assertFalse(notTen.clearsTable());
    }
    
    @Test
    @DisplayName("Should format card toString correctly")
    void testToString() {
        Card ace = new Card(1, Suit.HEARTS);
        Card jack = new Card(11, Suit.DIAMONDS);
        Card queen = new Card(12, Suit.CLUBS);
        Card king = new Card(13, Suit.SPADES);
        Card five = new Card(5, Suit.HEARTS);
        
        assertEquals("Ace of HEARTS", ace.toString());
        assertEquals("Jack of DIAMONDS", jack.toString());
        assertEquals("Queen of CLUBS", queen.toString());
        assertEquals("King of SPADES", king.toString());
        assertEquals("5 of HEARTS", five.toString());
    }

    @Test
    @DisplayName("Should play Ace on King (Ace is high)")
    void testAcePlaysOnKing() {
        Card king = new Card(13, Suit.HEARTS);
        Card ace = new Card(1, Suit.SPADES);
        
        assertTrue(ace.canPlayOn(king));
    }

    @Test
    @DisplayName("Should play 2 on any card (Special)")
    void testTwoPlaysOnAny() {
        Card king = new Card(13, Suit.HEARTS);
        Card two = new Card(2, Suit.SPADES);
        
        assertTrue(two.canPlayOn(king));
    }

    @Test
    @DisplayName("Should play any card on 2 (Reset)")
    void testAnyPlaysOnTwo() {
        Card two = new Card(2, Suit.HEARTS);
        Card three = new Card(3, Suit.SPADES);
        
        assertTrue(three.canPlayOn(two));
    }
}
