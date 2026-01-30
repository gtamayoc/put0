package com.game.server.put0.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameState model.
 */
@DisplayName("Game State Model Tests")
class GameStateTest {
    
    private GameState gameState;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState("test-game");
    }
    
    @Test
    @DisplayName("Should add player to game")
    void testAddPlayer() {
        Player player = new Player("p1", "Player1", false);
        gameState.addPlayer(player);
        
        assertEquals(1, gameState.getPlayers().size());
        assertEquals(player, gameState.getPlayers().get(0));
    }
    
    @Test
    @DisplayName("Should get current player")
    void testGetCurrentPlayer() {
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        
        gameState.addPlayer(player1);
        gameState.addPlayer(player2);
        
        assertEquals(player1, gameState.getCurrentPlayer());
        
        gameState.nextTurn();
        assertEquals(player2, gameState.getCurrentPlayer());
    }
    
    @Test
    @DisplayName("Should return null for current player when no players")
    void testGetCurrentPlayerWithNoPlayers() {
        assertNull(gameState.getCurrentPlayer());
    }
    
    @Test
    @DisplayName("Should get top card from table")
    void testGetTopCard() {
        assertNull(gameState.getTopCard());
        
        Card card1 = new Card(5, Suit.HEARTS);
        Card card2 = new Card(7, Suit.DIAMONDS);
        
        gameState.getTablePile().add(card1);
        gameState.getTablePile().add(card2);
        
        assertEquals(card2, gameState.getTopCard());
    }
    
    @Test
    @DisplayName("Should advance turn correctly")
    void testNextTurn() {
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        Player player3 = new Player("p3", "Player3", false);
        
        gameState.addPlayer(player1);
        gameState.addPlayer(player2);
        gameState.addPlayer(player3);
        
        assertEquals(0, gameState.getCurrentPlayerIndex());
        
        gameState.nextTurn();
        assertEquals(1, gameState.getCurrentPlayerIndex());
        
        gameState.nextTurn();
        assertEquals(2, gameState.getCurrentPlayerIndex());
        
        gameState.nextTurn();
        assertEquals(0, gameState.getCurrentPlayerIndex()); // Wraps around
    }
    
    @Test
    @DisplayName("Should clear table")
    void testClearTable() {
        gameState.getTablePile().add(new Card(5, Suit.HEARTS));
        gameState.getTablePile().add(new Card(7, Suit.DIAMONDS));
        
        assertEquals(2, gameState.getTablePile().size());
        
        gameState.clearTable();
        
        assertTrue(gameState.getTablePile().isEmpty());
    }
    
    @Test
    @DisplayName("Should detect when table should be cleared (4 of same value)")
    void testShouldClearTable() {
        gameState.getTablePile().add(new Card(5, Suit.HEARTS));
        gameState.getTablePile().add(new Card(5, Suit.DIAMONDS));
        gameState.getTablePile().add(new Card(5, Suit.CLUBS));
        
        assertFalse(gameState.shouldClearTable());
        
        gameState.getTablePile().add(new Card(5, Suit.SPADES));
        
        assertTrue(gameState.shouldClearTable());
    }
    
    @Test
    @DisplayName("Should not clear table with less than 4 cards")
    void testShouldNotClearTableWithLessThan4Cards() {
        gameState.getTablePile().add(new Card(5, Suit.HEARTS));
        gameState.getTablePile().add(new Card(5, Suit.DIAMONDS));
        
        assertFalse(gameState.shouldClearTable());
    }
    
    @Test
    @DisplayName("Should not clear table when last 4 cards are different")
    void testShouldNotClearTableWithDifferentValues() {
        gameState.getTablePile().add(new Card(5, Suit.HEARTS));
        gameState.getTablePile().add(new Card(5, Suit.DIAMONDS));
        gameState.getTablePile().add(new Card(5, Suit.CLUBS));
        gameState.getTablePile().add(new Card(7, Suit.SPADES)); // Different value
        
        assertFalse(gameState.shouldClearTable());
    }
    
    @Test
    @DisplayName("Should detect when game can start")
    void testCanStart() {
        assertFalse(gameState.canStart()); // No players
        
        gameState.addPlayer(new Player("p1", "Player1", false));
        assertFalse(gameState.canStart()); // Only 1 player
        
        gameState.addPlayer(new Player("p2", "Player2", false));
        assertTrue(gameState.canStart()); // 2 players, WAITING status
        
        gameState.setStatus(GameStatus.PLAYING);
        assertFalse(gameState.canStart()); // Not in WAITING status
    }
}
