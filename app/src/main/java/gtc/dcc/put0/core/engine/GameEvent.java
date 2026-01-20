package gtc.dcc.put0.core.engine;

public enum GameEvent {
    START_GAME,
    DEAL_CARDS,
    PLAY_CARD, // Triggers card play logic
    DRAW_CARD, // Player draws a card
    CLEAN_TABLE, // 4 cards or 10 -> table cleared
    END_TURN,
    PHASE_COMPLETE, // Transition to next phase
    GAME_WON,
    ERROR
}
