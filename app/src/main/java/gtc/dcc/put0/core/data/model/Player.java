package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;

import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.Suit;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("hand")
    private List<Card> hand = new ArrayList<>();

    @SerializedName("visibleCards")
    private List<Card> visibleCards = new ArrayList<>();

    @SerializedName("hiddenCards")
    private List<Card> hiddenCards = new ArrayList<>();

    @SerializedName("bot")
    private boolean isBot;

    @SerializedName("active")
    private boolean isActive = true;

    @SerializedName("cardCount")
    private int cardCount;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public List<Card> getVisibleCards() {
        return visibleCards;
    }

    public void setVisibleCards(List<Card> visibleCards) {
        this.visibleCards = visibleCards;
    }

    public List<Card> getHiddenCards() {
        return hiddenCards;
    }

    public void setHiddenCards(List<Card> hiddenCards) {
        this.hiddenCards = hiddenCards;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getCardCount() {
        return cardCount;
    }

    public void setCardCount(int cardCount) {
        this.cardCount = cardCount;
    }

    public boolean hasWon() {
        return (hand == null || hand.isEmpty()) &&
                (visibleCards == null || visibleCards.isEmpty()) &&
                (hiddenCards == null || hiddenCards.isEmpty());
    }
}
