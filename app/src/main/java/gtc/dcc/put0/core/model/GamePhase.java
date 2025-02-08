package gtc.dcc.put0.core.model;

public enum GamePhase {
    INITIAL_SETUP,
    PLAYER_TURN,
    OPPONENT_TURN,
    GAME_OVER;

    @Override
    public String toString() {
        switch (this) {
            case INITIAL_SETUP:
                return "Fase Inicial";
            case PLAYER_TURN:
                return "Turno del Jugador";
            case OPPONENT_TURN:
                return "Turno del Oponente";
            case GAME_OVER:
                return "Juego Terminado";
            default:
                return name();
        }
    }
}
