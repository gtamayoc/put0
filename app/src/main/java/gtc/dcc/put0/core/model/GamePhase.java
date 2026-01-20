package gtc.dcc.put0.core.model;

public enum GamePhase {
    SETUP,
    MAZO_INICIAL,
    SEGUNDO_MAZO,
    OCULTAS,
    FASE_EXTRA,
    GAME_OVER;

    @Override
    public String toString() {
        switch (this) {
            case SETUP:
                return "Preparando Juego";
            case MAZO_INICIAL:
                return "Mazo Inicial";
            case SEGUNDO_MAZO:
                return "Segundo Mazo";
            case OCULTAS:
                return "Cartas Ocultas";
            case FASE_EXTRA:
                return "Fase Extra";
            case GAME_OVER:
                return "Juego Terminado";
            default:
                return name();
        }
    }
}
