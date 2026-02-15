package gtc.dcc.put0.core.local;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.data.model.GameStatus;
import gtc.dcc.put0.core.data.model.Player;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.Suit;

/**
 * Mappers to convert between Android app models and game-core models.
 */
public class GameMapper {

    // --- Core -> Android ---

    public static GameState toAndroidState(com.game.core.model.GameState coreState) {
        if (coreState == null)
            return null;

        GameState androidState = new GameState();
        androidState.setGameId(coreState.getGameId());
        androidState.setCurrentPlayerIndex(coreState.getCurrentPlayerIndex());
        androidState.setStatus(toAndroidStatus(coreState.getStatus()));
        androidState.setWinnerId(coreState.getWinnerId());
        androidState.setLastAction(coreState.getLastAction());
        androidState.setDeckSize(coreState.getDeckSize());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidState.setPlayers(coreState.getPlayers().stream()
                    .map(GameMapper::toAndroidPlayer)
                    .collect(Collectors.toList()));

            androidState.setTablePile(coreState.getTablePile().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));

            androidState.setMainDeck(coreState.getMainDeck().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));

            androidState.setDiscardPile(coreState.getDiscardPile().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));
        }

        return androidState;
    }

    public static Player toAndroidPlayer(com.game.core.model.Player corePlayer) {
        if (corePlayer == null)
            return null;
        Player androidPlayer = new Player();
        androidPlayer.setId(corePlayer.getId());
        androidPlayer.setName(corePlayer.getName());
        androidPlayer.setBot(corePlayer.isBot());
        androidPlayer.setActive(corePlayer.isActive());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidPlayer.setHand(corePlayer.getHand().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));

            androidPlayer.setVisibleCards(corePlayer.getVisibleCards().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));

            androidPlayer.setHiddenCards(corePlayer.getHiddenCards().stream()
                    .map(GameMapper::toAndroidCard)
                    .collect(Collectors.toList()));
        }

        androidPlayer.setCardCount(corePlayer.getCardCount());

        return androidPlayer;
    }

    public static gtc.dcc.put0.core.model.Card toAndroidCard(com.game.core.model.Card coreCard) {
        if (coreCard == null)
            return null;
        gtc.dcc.put0.core.model.Card androidCard = new gtc.dcc.put0.core.model.Card(
                toAndroidSuit(coreCard.getSuit()),
                coreCard.getValue(),
                coreCard.isHidden());
        androidCard.setInstanceId(coreCard.getInstanceId());
        return androidCard;
    }

    public static Suit toAndroidSuit(com.game.core.model.Suit coreSuit) {
        if (coreSuit == null)
            return null;
        return Suit.valueOf(coreSuit.name());
    }

    public static GameStatus toAndroidStatus(com.game.core.model.GameStatus coreStatus) {
        if (coreStatus == null)
            return null;
        return GameStatus.valueOf(coreStatus.name());
    }

    // --- Android -> Core ---

    public static com.game.core.model.Card toCoreCard(gtc.dcc.put0.core.model.Card androidCard) {
        if (androidCard == null)
            return null;
        com.game.core.model.Card coreCard = new com.game.core.model.Card(
                androidCard.getRankValue(),
                toCoreSuit(androidCard.getSuit()));
        coreCard.setHidden(androidCard.isHidden());
        coreCard.setInstanceId(androidCard.getInstanceId());
        return coreCard;
    }

    public static com.game.core.model.Suit toCoreSuit(Suit androidSuit) {
        if (androidSuit == null)
            return null;
        return com.game.core.model.Suit.valueOf(androidSuit.name());
    }
}
