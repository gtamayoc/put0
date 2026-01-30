package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Card implements Serializable {
    @SerializedName("value")
    private int value;
    @SerializedName("suit")
    private Suit suit;

    public Card(int value, Suit suit) {
        this.value = value;
        this.suit = suit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Suit getSuit() {
        return suit;
    }

    public void setSuit(Suit suit) {
        this.suit = suit;
    }

    @Override
    public String toString() {
        return getValueString() + " of " + suit;
    }

    private String getValueString() {
        switch (value) {
            case 1:
                return "Ace";
            case 11:
                return "Jack";
            case 12:
                return "Queen";
            case 13:
                return "King";
            default:
                return String.valueOf(value);
        }
    }
}
