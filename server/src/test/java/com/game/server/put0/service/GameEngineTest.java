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
        assertEquals(86, game.getMainDeck().size());
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
            assertEquals(0, game.getTablePile().size());
        } else {
            assertEquals(1, game.getTablePile().size());
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
        game.getTablePile().add(new Card(5, Suit.HEARTS));
        game.getTablePile().add(new Card(7, Suit.DIAMONDS));
        
        // Find and play a 10
        Player currentPlayer = game.getCurrentPlayer();
        Card ten = new Card(10, Suit.CLUBS);
        
        // Ensure player has it in HAND (Phase 1)
        currentPlayer.getHand().clear(); // Clear existing hand to be sure
        currentPlayer.addCard(ten);
        
        int currentPlayerIndex = game.getCurrentPlayerIndex();
        gameEngine.playCard(gameId, currentPlayer.getId(), ten);
        
        // Table should be cleared
        assertTrue(game.getTablePile().isEmpty());
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
        game.getTablePile().add(new Card(13, Suit.SPADES)); // King
        
        // ensure deck has cards
        if (game.getMainDeck().isEmpty()) {
             game.getMainDeck().add(new Card(5, Suit.CLUBS));
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
        game.getMainDeck().clear();
        
        // Give one winning card
        Card lastCard = new Card(14, Suit.SPADES); // strong card
        currentPlayer.addCard(lastCard);
        
        // Ensure it can be played (clear table or low card)
        game.getTablePile().clear();
        
        gameEngine.playCard(gameId, currentPlayer.getId(), lastCard);
        
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(currentPlayer.getId(), game.getWinnerId());
    }

    @Test
    @DisplayName("Should move visible cards to hand when deck and hand are empty (Phase 3 transition)")
    void testPhase3Transition() {
        String gameId = "test-game-13";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        GameState game = gameEngine.getGame(gameId);
        
        // Setup Phase 3 transition scenario
        Player currentPlayer = game.getCurrentPlayer();
        currentPlayer.getHand().clear();
        game.getMainDeck().clear();
        
        // Ensure visible cards exist
        Card visibleCard = new Card(5, Suit.HEARTS);
        currentPlayer.getVisibleCards().clear();
        currentPlayer.getVisibleCards().add(visibleCard);
        
        // Add ONE card to hand to play it and trigger replenishHand
        Card lastHandCard = new Card(10, Suit.CLUBS); 
        currentPlayer.addCard(lastHandCard);
        
        gameEngine.playCard(gameId, currentPlayer.getId(), lastHandCard);
        
        // After playing the last hand card, replenishHand is called.
        // It should have moved visibleCard to hand.
        assertTrue(currentPlayer.getVisibleCards().isEmpty());
        assertEquals(1, currentPlayer.getHand().size());
        assertEquals(visibleCard, currentPlayer.getHand().get(0));
    }

    @Test
    @DisplayName("Should handle Phase 4 transition and regression on failed blind play")
    void testPhase4TransitionAndRegression() {
        String gameId = "test-game-14";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        GameState game = gameEngine.getGame(gameId);
        
        Player currentPlayer = game.getCurrentPlayer();
        currentPlayer.getHand().clear();
        currentPlayer.getVisibleCards().clear();
        currentPlayer.getHiddenCards().clear(); // Clear initial dealt hidden cards
        game.getMainDeck().clear();
        
        // Phase 4 Setup: only hidden cards remain
        Card c1 = new Card(3, Suit.HEARTS); // unplayable if top is high
        Card c2 = new Card(4, Suit.CLUBS);
        currentPlayer.getHiddenCards().add(c1);
        currentPlayer.getHiddenCards().add(c2);
        
        // One card in hand to trigger replenishHand (from hidden)
        Card trigger = new Card(10, Suit.SPADES);
        currentPlayer.addCard(trigger);
        
        gameEngine.playCard(gameId, currentPlayer.getId(), trigger);
        
        // Verify F4 Transition
        assertEquals(2, currentPlayer.getHand().size());
        assertTrue(currentPlayer.getHiddenCards().isEmpty());
        assertTrue(currentPlayer.getHand().get(0).isHidden());
        assertTrue(currentPlayer.getHand().get(1).isHidden());
        
        // Setup table with high card to force failure
        game.getTablePile().add(new Card(13, Suit.SPADES)); // King
        
        // Attempt blind play (c1 = 3)
        gameEngine.playCard(gameId, currentPlayer.getId(), c1);
        
        // Verify Regression
        // 1. Failed card (c1) is revealed and now in player's hand (collected from table)
        // Actually, in the engine, c1 is added to table, then table is collected.
        assertTrue(currentPlayer.getHand().contains(c1));
        assertFalse(c1.isHidden());
        
        // 2. Remaining hidden card (c2) should have moved back to hiddenCards pile
        assertTrue(currentPlayer.getHand().stream().noneMatch(Card::isHidden));
        assertEquals(1, currentPlayer.getHiddenCards().size());
        assertEquals(c2, currentPlayer.getHiddenCards().get(0));
    }

    @Test
    @DisplayName("Should detect victory in Phase 4")
    void testPhase4Victory() {
        String gameId = "test-game-15";
        gameEngine.createGame(gameId);
        
        Player player1 = new Player("p1", "Player1", false);
        Player player2 = new Player("p2", "Player2", false);
        gameEngine.addPlayer(gameId, player1);
        gameEngine.addPlayer(gameId, player2);
        
        gameEngine.startGame(gameId);
        GameState game = gameEngine.getGame(gameId);
        
        Player currentPlayer = game.getCurrentPlayer();
        currentPlayer.getHand().clear();
        currentPlayer.getVisibleCards().clear();
        currentPlayer.getHiddenCards().clear();
        game.getMainDeck().clear();
        
        // Final hidden card
        Card lastCard = new Card(14, Suit.SPADES); // Ace
        lastCard.setHidden(true);
        currentPlayer.addCard(lastCard);
        
        gameEngine.playCard(gameId, currentPlayer.getId(), lastCard);
        
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(currentPlayer.getId(), game.getWinnerId());
    }
}
