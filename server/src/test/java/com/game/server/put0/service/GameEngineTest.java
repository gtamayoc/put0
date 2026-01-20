package com.game.server.put0.service;

import com.game.server.put0.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameEngine service.
 */
@DisplayName("Game Engine Tests")
class GameEngineTest {
    
    private GameEngine gameEngine;
    
    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
    }
    
    @Test
    @DisplayName("Should create a new game")
    void testCreateGame() {
        String gameId = "test-game-1";
        GameState game = gameEngine.createGame(gameId);
        
        assertNotNull(game);
        assertEquals(gameId, game.getGameId());
        assertEquals(GameStatus.WAITING, game.getStatus());
        assertTrue(game.getPlayers().isEmpty());
    }
    
    @Test
    @DisplayName("Should add player to game")
    void testAddPlayer() {
        String gameId = "test-game-2";
        gameEngine.createGame(gameId);
        
        Player player = new Player("player-1", "TestPlayer", false);
        gameEngine.addPlayer(gameId, player);
        
        GameState game = gameEngine.getGame(gameId);
        assertEquals(1, game.getPlayers().size());
        assertEquals("TestPlayer", game.getPlayers().get(0).getName());
    }
    
    @Test
    @DisplayName("Should not add player to game in progress")
    void testCannotAddPlayerToGameInProgress() {
        String gameId = "test-game-3";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        Player player3 = new Player("p3", "Player3", false);
        assertThrows(IllegalStateException.class, () -> {
            gameEngine.addPlayer(gameId, player3);
        });
    }
    
    @Test
    @DisplayName("Should start game with at least 2 players")
    void testStartGame() {
        String gameId = "test-game-4";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        assertEquals(GameStatus.PLAYING, game.getStatus());
        assertTrue(game.getPlayers().get(0).getCardCount() > 0);
        assertTrue(game.getPlayers().get(1).getCardCount() > 0);
    }
    
    @Test
    @DisplayName("Should not start game with less than 2 players")
    void testCannotStartGameWithOnePlayer() {
        String gameId = "test-game-5";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        gameEngine.addPlayer(gameId, player1);
        
        assertThrows(IllegalStateException.class, () -> {
            gameEngine.startGame(gameId);
        });
    }
    
    @Test
    @DisplayName("Should deal 3+3+3 cards to all players")
    void testDealCardsEvenly() {
        String gameId = "test-game-6";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        int cards1 = game.getPlayers().get(0).getCardCount();
        int cards2 = game.getPlayers().get(1).getCardCount();
        
        assertEquals(cards1, cards2);
        assertEquals(9, cards1); // 3 Hidden + 3 Visible + 3 Hand
        
        // 104 cards total - 18 dealt = 86 remaining
        assertEquals(86, game.getDeck().size());
    }
    
    @Test
    @DisplayName("Should play valid card")
    void testPlayValidCard() {
        String gameId = "test-game-7";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        Player currentPlayer = game.getCurrentPlayer();
        
        // Find a card that is NOT a 10 (to avoid clearing the table)
        // Must pick from HAND as per phase rules
        Card cardToPlay = currentPlayer.getHand().stream()
                .filter(card -> card.getValue() != 10)
                .findFirst()
                .orElse(currentPlayer.getHand().get(0));
        
        gameEngine.playCard(gameId, currentPlayer.getId(), cardToPlay);
        
        // If we played a 10, table will be empty, otherwise it should have 1 card
        if (cardToPlay.getValue() == 10) {
            assertEquals(0, game.getTable().size());
        } else {
            assertEquals(1, game.getTable().size());
            assertEquals(cardToPlay, game.getTopCard());
        }
    }
    
    @Test
    @DisplayName("Should not allow playing out of turn")
    void testCannotPlayOutOfTurn() {
        String gameId = "test-game-8";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        Player notCurrentPlayer = game.getPlayers().get(1);
        Card card = notCurrentPlayer.getHand().get(0);
        
        assertThrows(IllegalStateException.class, () -> {
            gameEngine.playCard(gameId, notCurrentPlayer.getId(), card);
        });
    }
    
    @Test
    @DisplayName("Should clear table when 10 is played")
    void testTableClearsOn10() {
        String gameId = "test-game-9";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        
        // Add some cards to the table first
        game.getTable().add(new Card(5, Suit.HEARTS));
        game.getTable().add(new Card(7, Suit.DIAMONDS));
        
        // Find and play a 10
        Player currentPlayer = game.getCurrentPlayer();
        Card ten = new Card(10, Suit.CLUBS);
        
        // Ensure player has it in HAND (Phase 1)
        currentPlayer.getHand().clear(); // Clear existing hand to be sure
        currentPlayer.addCard(ten);
        
        int currentPlayerIndex = game.getCurrentPlayerIndex();
        gameEngine.playCard(gameId, currentPlayer.getId(), ten);
        
        // Table should be cleared
        assertTrue(game.getTable().isEmpty());
        // Same player should still have turn (after clearing)
        assertEquals(currentPlayerIndex, game.getCurrentPlayerIndex());
    }
    
    @Test
    @DisplayName("Should advance turn after playing card")
    void testTurnAdvancement() {
        String gameId = "test-game-10";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        int initialPlayerIndex = game.getCurrentPlayerIndex();
        
        Player currentPlayer = game.getCurrentPlayer();
        Card card = currentPlayer.getHand().get(0);
        
        gameEngine.playCard(gameId, currentPlayer.getId(), card);
        
        int newPlayerIndex = game.getCurrentPlayerIndex();
        assertNotEquals(initialPlayerIndex, newPlayerIndex);
    }
    
    @Test
    @DisplayName("Should draw card when no valid moves")
    void testDrawCard() {
        String gameId = "test-game-11";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        
        // Set up scenario where player has no valid cards
        Player currentPlayer = game.getCurrentPlayer();
        currentPlayer.getHand().clear();
        currentPlayer.getVisibleCards().clear(); // Must clear other phases too
        currentPlayer.getHiddenCards().clear();
        
        // Add unplayable card to hand
        currentPlayer.addCard(new Card(3, Suit.HEARTS)); 
        // Note: 2 is special (always playable), so use 3 vs King
        
        // Set high card on table
        game.getTable().add(new Card(13, Suit.SPADES)); // King
        
        // ensure deck has cards
        if (game.getDeck().isEmpty()) {
             game.getDeck().add(new Card(5, Suit.CLUBS));
        }
        
        int initialHandSize = currentPlayer.getCardCount();
        
        gameEngine.drawCard(gameId, currentPlayer.getId());
        
        assertEquals(initialHandSize + 1, currentPlayer.getCardCount());
    }
    
    @Test
    @DisplayName("Should detect winner when player has no cards")
    void testWinDetection() {
        String gameId = "test-game-12";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        
        GameState game = gameEngine.getGame(gameId);
        Player currentPlayer = game.getCurrentPlayer();
        
        // Remove all cards from ALL piles
        currentPlayer.getHand().clear();
        currentPlayer.getVisibleCards().clear();
        currentPlayer.getHiddenCards().clear();
        
        // Deck must be empty to avoid auto-draw on play
        game.getDeck().clear();
        
        // Give one winning card
        Card lastCard = new Card(14, Suit.SPADES); // strong card
        currentPlayer.addCard(lastCard);
        
        // Ensure it can be played (clear table or low card)
        game.getTable().clear();
        
        gameEngine.playCard(gameId, currentPlayer.getId(), lastCard);
        
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(currentPlayer.getId(), game.getWinnerId());
    }
}
