package gtc.dcc.put0.core.model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class Card {
    @SerializedName("suit")
    private final Suit suit;

    // Server sends "value": 10 (int)
    @SerializedName("value")
    private final int rankValue;

    // Internal Rank object
    private final Rank rank;

    private int resourceId;
    private boolean faceUp;
    private boolean isSelected;

    @SerializedName("hidden")
    private boolean hidden;

    private boolean isPlaceholder = false;

    public Card() {
        this.suit = Suit.CLUBS; // Default for placeholder
        this.rankValue = 0;
        this.rank = Rank.TWO; // Using Rank.TWO as a low-power default
        this.isPlaceholder = true;
    }

    public Card(Suit suit, Rank rank, int resourceId) {
        this.suit = suit;
        this.rank = rank;
        this.rankValue = rank != null ? rank.getValue() : 0;
        this.resourceId = resourceId;
        this.faceUp = false;
        this.isSelected = false;
    }

    public Card(Suit suit, int rankValue, boolean hidden) {
        this.suit = suit;
        this.rankValue = rankValue;
        this.rank = mapValueToRank(rankValue);
        this.hidden = hidden;
        this.resourceId = 0;
        this.faceUp = false;
        this.isSelected = false;
    }

    public Card(Suit suit, Rank rank) {
        this(suit, rank, 0);
    }

    public Card(Suit suit, int rankValue) {
        this.suit = suit;
        this.rankValue = rankValue;
        this.rank = mapValueToRank(rankValue);
        this.resourceId = 0;
    }

    // Helper to map int to Rank
    private static Rank mapValueToRank(int value) {
        for (Rank r : Rank.values()) {
            if (r.getValue() == value)
                return r;
        }
        return Rank.ACE; // Default or null?
    }

    // Getters
    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        // If constructed via Gson, rank might be null if we don't init it.
        // Wait, Gson sets fields directly using Unsafe.
        // If 'rank' is not in JSON, it remains null.
        // So this approach fails unless we calculate it lazily.
        if (rank == null) {
            return mapValueToRank(rankValue);
        }
        return rank;
    }

    public String getValue() {
        return String.valueOf(getRankValue());
    }

    public int getRankValue() {
        // Ensure we use the deserialized value
        return rankValue;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isPlaceholder() {
        return isPlaceholder;
    }

    public void setPlaceholder(boolean placeholder) {
        isPlaceholder = placeholder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Card card = (Card) o;
        return suit == card.suit && getRank() == card.getRank();
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, getRank());
    }

    @Override
    public String toString() {
        return "Card{" +
                "suit=" + suit +
                ", rank=" + getRank() +
                ", value=" + rankValue +
                '}';
    }
}