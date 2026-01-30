package com.game.core.engine;

import com.game.core.model.Card;
import com.game.core.model.GameState;
import com.game.core.model.GameStatus;
import com.game.core.model.Player;
import com.game.core.model.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine engine;
    private GameState game;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        game = engine.createGame("test-game");
    }

    @Test
    void testStartGame() {
        engine.addPlayer(game, new Player("p1", "Player 1", false));
        engine.addPlayer(game, new Player("p2", "Bot 1", true));
        
        engine.startGame(game);
        
        assertEquals(GameStatus.PLAYING, game.getStatus());
        // 2 French decks = 104 cards. 2 players * 9 cards = 18 dealt.
        // 104 - 18 = 86
        assertEquals(86, game.getMainDeck().size());
        
        for (Player p : game.getPlayers()) {
            assertEquals(3, p.getHand().size(), "Hand size for " + p.getName());
            assertEquals(3, p.getVisibleCards().size(), "Visible size for " + p.getName());
            assertEquals(3, p.getHiddenCards().size(), "Hidden size for " + p.getName());
        }
    }

    @Test
    void testPlayValidCard() {
        Player p1Obj = new Player("p1", "Player 1", false);
        engine.addPlayer(game, p1Obj);
        engine.addPlayer(game, new Player("p2", "Bot 1", true));
        engine.startGame(game);
        
        // Fetch player from game state to be certain
        Player p1 = game.getPlayers().get(0);
        game.setCurrentPlayerIndex(0);
        
        assertFalse(p1.getHand().isEmpty(), "Player 1 hand should not be empty after start");
        
        Card cardToPlay = p1.getHand().get(0);
        int handSizeBefore = p1.getHand().size();
        
        engine.playCard(game, "p1", cardToPlay);
        
        // After playing, hand size might still be 3 because of replenish if deck not empty
        assertEquals(3, p1.getHand().size());
        
        // Deck should be 85 because one card was drawn to replenish
        assertEquals(85, game.getMainDeck().size());
        
        // Table should have the played card
        assertEquals(cardToPlay, game.getTopCard());
    }
}
