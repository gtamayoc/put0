package gtc.dcc.put0.core.engine;

import org.squirrelframework.foundation.fsm.AnonymousAction;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

import gtc.dcc.put0.core.model.GamePhase;

@StateMachineParameters(stateType = GamePhase.class, eventType = GameEvent.class, contextType = GameContext.class)
public class Put0StateMachine extends AbstractUntypedStateMachine {

    protected void defineTransitions(UntypedStateMachineBuilder builder) {
        // SETUP -> MAZO_INICIAL
        builder.externalTransition().from(GamePhase.SETUP).to(GamePhase.MAZO_INICIAL).on(GameEvent.START_GAME)
                .callMethod("onStartGame");

        // MAZO_INICIAL -> SEGUNDO_MAZO (Example transition, rule dependent)
        builder.externalTransition().from(GamePhase.MAZO_INICIAL).to(GamePhase.SEGUNDO_MAZO)
                .on(GameEvent.PHASE_COMPLETE);

        // ... Add more transitions
    }

    public void onStartGame(GamePhase from, GamePhase to, GameEvent event, GameContext context) {
        // Logic for starting game
        System.out.println("Game Started: " + context);
    }
}
