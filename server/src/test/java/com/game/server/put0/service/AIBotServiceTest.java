package com.game.server.put0.service;

import com.game.server.put0.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AIBotServiceTest {

    @Mock
    private GameEngine gameEngine;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private AIBotService aiBotService;

    private Player otherPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aiBotService = new AIBotService(gameEngine, messagingTemplate);
        otherPlayer = new Player("player-1", "Human", false);
    }

    @Test
    void testBotEatsTableWhenCannotPlay() {
        // Arrange
        String gameId = "test-game";
        String botId = "bot-1";
        
        GameState game = mock(GameState.class);
        Player bot = new Player(botId, "Bot 1", true);
        
        // Bot has only a '3', cannot beat an 'Ace'
        Card lowCard = new Card(3, Suit.CLUBS);
        bot.getHand().add(lowCard);
        
        when(game.getPlayers()).thenReturn(Collections.singletonList(bot));
        when(game.getTopCard()).thenReturn(new Card(1, Suit.HEARTS));
        when(game.getTablePile()).thenReturn(Collections.singletonList(new Card(1, Suit.HEARTS)));
        when(game.getStatus()).thenReturn(GameStatus.PLAYING);
        
        // In this test, we call makeMove(botId) directly. 
        // After collectTable, it calls checkAndMakeBotMove, which calls getCurrentPlayer.
        // If we return otherPlayer, it won't loop.
        when(game.getCurrentPlayer()).thenReturn(otherPlayer);
        
        when(gameEngine.getGame(gameId)).thenReturn(game);

        // Act
        aiBotService.makeMove(gameId, botId);

        // Assert: Bot should collect table
        verify(gameEngine).collectTable(eq(gameId), eq(botId));
    }

    @Test
    void testBotPlaysCardAndTriggersNext() {
        // Arrange
        String gameId = "test-game";
        String botId = "bot-1";
        
        GameState game = mock(GameState.class);
        Player bot = new Player(botId, "Bot 1", true);
        Card card = new Card(5, Suit.CLUBS);
        bot.getHand().add(card);
        
        when(game.getPlayers()).thenReturn(Collections.singletonList(bot));
        when(game.getStatus()).thenReturn(GameStatus.PLAYING);
        
        // After playCard, it calls getCurrentPlayer to check for extra turn.
        // If we return otherPlayer, it won't loop.
        when(game.getCurrentPlayer()).thenReturn(otherPlayer);
        
        when(gameEngine.getGame(gameId)).thenReturn(game);

        // Act
        aiBotService.makeMove(gameId, botId);

        // Assert
        verify(gameEngine).playCard(eq(gameId), eq(botId), eq(card));
    }

    @Test
    void testBotDrawsIfTableEmptyAndCannotPlay() {
        // Arrange
        String gameId = "test-game";
        String botId = "bot-1";
        
        GameState game = mock(GameState.class);
        Player bot = new Player(botId, "Bot 1", true);
        
        when(game.getPlayers()).thenReturn(Collections.singletonList(bot));
        when(game.getTablePile()).thenReturn(new ArrayList<>()); // Table is empty
        when(game.getStatus()).thenReturn(GameStatus.PLAYING);
        
        // After drawCard, it calls checkAndMakeBotMove -> getCurrentPlayer.
        when(game.getCurrentPlayer()).thenReturn(otherPlayer);
        
        when(gameEngine.getGame(gameId)).thenReturn(game);

        // Act
        aiBotService.makeMove(gameId, botId);

        // Assert
        verify(gameEngine).drawCard(eq(gameId), eq(botId));
    }

    @Test
    void testBotGetsExtraTurnOnTableClear() {
        // Arrange
        String gameId = "test-game";
        String botId = "bot-1";
        
        GameState game = mock(GameState.class);
        Player bot = new Player(botId, "Bot 1", true);
        Card six = new Card(6, Suit.CLUBS);
        Card anotherCard = new Card(7, Suit.DIAMONDS);
        bot.getHand().add(six);
        bot.getHand().add(anotherCard);
        
        when(game.getPlayers()).thenReturn(Collections.singletonList(bot));
        when(game.getStatus()).thenReturn(GameStatus.PLAYING);
        
        // 1st call (after playCard(six)): returns bot -> loops
        // 2nd call (after playCard(anotherCard)): returns otherPlayer -> stops
        when(game.getCurrentPlayer()).thenReturn(bot).thenReturn(otherPlayer);
        
        when(gameEngine.getGame(gameId)).thenReturn(game);

        // Mock playCard to actually remove the card from the bot's hand
        doAnswer(invocation -> {
            Card card = invocation.getArgument(2);
            bot.getHand().remove(card);
            return null;
        }).when(gameEngine).playCard(eq(gameId), eq(botId), any(Card.class));

        // Act
        aiBotService.makeMove(gameId, botId);

        // Assert: It should have played BOTH cards
        verify(gameEngine).playCard(eq(gameId), eq(botId), eq(six));
        verify(gameEngine).playCard(eq(gameId), eq(botId), eq(anotherCard));
    }
}
