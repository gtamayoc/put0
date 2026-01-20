package gtc.dcc.put0.core.rules;

import gtc.dcc.put0.core.engine.GameContext;

public interface Regla {
    boolean aplica(GameContext ctx);

    void ejecutar(GameContext ctx);
}
