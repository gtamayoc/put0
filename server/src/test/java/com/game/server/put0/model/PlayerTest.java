package com.game.server.put0.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player model.
 */
@DisplayName("Player Model Tests")
class PlayerTest {
    
    private Player player;
    
    @BeforeEach
    void setUp() {
        player = new Player("p1", "TestPlayer", false);
    }
    
    @Test
    @DisplayName("Should add card to hand")
    void testAddCard() {
        Card card = new Card(5, Suit.HEARTS);
        player.addCard(card);
        
        assertEquals(1, player.getCardCount());
        assertTrue(player.getHand().contains(card));
    }
    
    @Test
    @DisplayName("Should remove card from hand")
    void testRemoveCard() {
        Card card = new Card(5, Suit.HEARTS);
        player.addCard(card);
        
        boolean removed = player.removeCard(card);
        
        assertTrue(removed);
        assertEquals(0, player.getCardCount());
        assertFalse(player.getHand().contains(card));
    }
    
    @Test
    @DisplayName("Should return false when removing non-existent card")
    void testRemoveNonExistentCard() {
        Card card = new Card(5, Suit.HEARTS);
        
        boolean removed = player.removeCard(card);
        
        assertFalse(removed);
    }
    
    @Test
    @DisplayName("Should detect when player has cards")
    void testHasCards() {
        assertFalse(player.hasCards());
        
        player.addCard(new Card(5, Suit.HEARTS));
        
        assertTrue(player.hasCards());
    }
    
    @Test
    @DisplayName("Should detect when player has won")
    void testHasWon() {
        // Player starts with no cards, so initially has won
        assertTrue(player.hasWon());
        
        player.addCard(new Card(5, Suit.HEARTS));
        assertFalse(player.hasWon());
        
        player.removeCard(player.getHand().get(0));
        assertTrue(player.hasWon());
    }
    
    @Test
    @DisplayName("Should find playable cards")
    void testGetPlayableCards() {
        player.addCard(new Card(3, Suit.HEARTS));
        player.addCard(new Card(7, Suit.DIAMONDS));
        player.addCard(new Card(10, Suit.CLUBS));
        
        Card tableCard = new Card(5, Suit.SPADES);
        
        List<Card> playableCards = player.getPlayableCards(tableCard);
        
        // Should be able to play 7 and 10 (not 3)
        assertEquals(2, playableCards.size());
        assertTrue(playableCards.stream().anyMatch(c -> c.getValue() == 7));
        assertTrue(playableCards.stream().anyMatch(c -> c.getValue() == 10));
    }
    
    @Test
    @DisplayName("Should find all cards playable on empty table")
    void testGetPlayableCardsOnEmptyTable() {
        player.addCard(new Card(3, Suit.HEARTS));
        player.addCard(new Card(7, Suit.DIAMONDS));
        player.addCard(new Card(10, Suit.CLUBS));
        
        List<Card> playableCards = player.getPlayableCards(null);
        
        assertEquals(3, playableCards.size());
    }
    
    @Test
    @DisplayName("Should identify bot players")
    void testBotIdentification() {
        Player humanPlayer = new Player("p1", "Human", false);
        Player botPlayer = new Player("p2", "Bot", true);
        
        assertFalse(humanPlayer.isBot());
        assertTrue(botPlayer.isBot());
    }
}
