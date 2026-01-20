package gtc.dcc.put0.core.utils;

import android.content.Context;
import gtc.dcc.put0.core.utils.CoreLogger;

import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.Suit;

public class DeckUtils {
    private static final String TAG = "DeckUtils";

    public static int getCardResourceId(Context context, Card card) {
        if (card == null)
            return 0;

        String rankStr;
        switch (card.getRankValue()) { // Use getRankValue() which returns int
            case 1:
                rankStr = "ace";
                break;
            case 11:
                rankStr = "jack";
                break;
            case 12:
                rankStr = "queen";
                break;
            case 13:
                rankStr = "king";
                break;
            case 14:
                rankStr = "ace";
                break; // Handle Ace as 14 if server sends it
            default:
                rankStr = String.valueOf(card.getRankValue());
        }

        String suitStr = card.getSuit().name().toLowerCase();
        String resourceName = "card_" + rankStr + "_" + suitStr;

        int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resourceId == 0) {
            CoreLogger.e("Missing resource: " + resourceName);
            // Fallback to base or error card
            if (context.getResources().getIdentifier("base", "drawable", context.getPackageName()) != 0) {
                return context.getResources().getIdentifier("base", "drawable", context.getPackageName());
            }
        }
        return resourceId;
    }

    public static int getCardValue(int value) {
        return value == 1 ? 14 : value; // Treat Ace (1) as high (14) if needed, or adjust based on game rules
    }

    public static int getCardValue(String value) {
        if (value == null)
            return 0;
        switch (value.toLowerCase()) {
            case "as":
            case "ace":
            case "1":
                return 14;
            case "jota":
            case "jack":
            case "11":
                return 11;
            case "reina":
            case "queen":
            case "12":
                return 12;
            case "rey":
            case "king":
            case "13":
                return 13;
            default:
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 0;
                }
        }
    }
}
