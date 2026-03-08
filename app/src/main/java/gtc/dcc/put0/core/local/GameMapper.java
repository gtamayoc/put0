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
        androidState.setGameStartTime(coreState.getGameStartTime());
        androidState.setWonByAbandonment(coreState.isWonByAbandonment());

        // Use loops instead of Streams for backward compatibility (API < 24)
        List<Player> androidPlayers = new ArrayList<>();
        if (coreState.getPlayers() != null) {
            for (com.game.core.model.Player p : coreState.getPlayers()) {
                androidPlayers.add(toAndroidPlayer(p));
            }
        }
        androidState.setPlayers(androidPlayers);

        List<gtc.dcc.put0.core.model.Card> androidTable = new ArrayList<>();
        if (coreState.getTablePile() != null) {
            for (com.game.core.model.Card c : coreState.getTablePile()) {
                androidTable.add(toAndroidCard(c));
            }
        }
        androidState.setTablePile(androidTable);

        List<gtc.dcc.put0.core.model.Card> androidMain = new ArrayList<>();
        if (coreState.getMainDeck() != null) {
            for (com.game.core.model.Card c : coreState.getMainDeck()) {
                androidMain.add(toAndroidCard(c));
            }
        }
        androidState.setMainDeck(androidMain);

        List<gtc.dcc.put0.core.model.Card> androidDiscard = new ArrayList<>();
        if (coreState.getDiscardPile() != null) {
            for (com.game.core.model.Card c : coreState.getDiscardPile()) {
                androidDiscard.add(toAndroidCard(c));
            }
        }
        androidState.setDiscardPile(androidDiscard);

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
        androidPlayer.setConnected(corePlayer.isConnected());
        androidPlayer.setDisconnectedTime(corePlayer.getDisconnectedTime());

        List<gtc.dcc.put0.core.model.Card> hand = new ArrayList<>();
        if (corePlayer.getHand() != null) {
            for (com.game.core.model.Card c : corePlayer.getHand()) {
                hand.add(toAndroidCard(c));
            }
        }
        androidPlayer.setHand(hand);

        List<gtc.dcc.put0.core.model.Card> visible = new ArrayList<>();
        if (corePlayer.getVisibleCards() != null) {
            for (com.game.core.model.Card c : corePlayer.getVisibleCards()) {
                visible.add(toAndroidCard(c));
            }
        }
        androidPlayer.setVisibleCards(visible);

        List<gtc.dcc.put0.core.model.Card> hidden = new ArrayList<>();
        if (corePlayer.getHiddenCards() != null) {
            for (com.game.core.model.Card c : corePlayer.getHiddenCards()) {
                hidden.add(toAndroidCard(c));
            }
        }
        androidPlayer.setHiddenCards(hidden);

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
