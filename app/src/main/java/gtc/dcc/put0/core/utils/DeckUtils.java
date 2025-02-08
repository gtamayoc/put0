package gtc.dcc.put0.core.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gtc.dcc.put0.core.model.Card;

public class DeckUtils {

    private static final String TAG = "DeckUtils";

    /**
     * Crea un mazo completo con las cartas de póker tradicionales.
     *
     * @param context Contexto de la aplicación para acceder a los recursos.
     * @param shuffle Si es verdadero, el mazo se barajará automáticamente.
     * @return Lista de cartas (`Card`) generada dinámicamente.
     */
    public static List<Card> createDeck(Context context, boolean shuffle) {
        List<Card> deck = new ArrayList<>();
        String[] suits = {"spades", "hearts", "diamonds", "clubs"};
        String[] ranks = {"ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king"};

        for (String suit : suits) {
            for (String rank : ranks) {
                // Construye el nombre del recurso (ejemplo: card_ace_spades)
                String resourceName = "card_" + rank + "_" + suit;
                int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

                if (resourceId != 0) {
                    deck.add(new Card(suit, rank, resourceId));
                } else {
                    Log.e(TAG, "Falta recurso: " + resourceName);
                }
            }
        }

        if (shuffle) {
            shuffleDeck(deck);
        }

        return deck;
    }

    /**
     * Baraja un mazo de cartas.
     *
     * @param deck Lista de cartas a barajar.
     */
    public static void shuffleDeck(List<Card> deck) {
        Collections.shuffle(deck);
    }

    /**
     * Obtiene una carta específica del mazo.
     *
     * @param deck  Mazo de cartas.
     * @param suit  Palo de la carta.
     * @param rank  Rango de la carta.
     * @return La carta buscada, o null si no está en el mazo.
     */
    public static Card getCard(List<Card> deck, String suit, String rank) {
        for (Card card : deck) {
            if (card.getSuit().equals(suit) && card.getValue().equals(rank)) { // Cambiado getRank() por getValue()
                return card;
            }
        }
        return null;
    }

    /**
     * Lista todas las cartas del mazo.
     *
     * @param deck Lista de cartas.
     * @return Cadena con información de las cartas.
     */
    public static String listCards(List<Card> deck) {
        StringBuilder builder = new StringBuilder();
        for (Card card : deck) {
            builder.append(card.getValue()) // Cambiado getRank() por getValue()
                    .append(" of ")
                    .append(card.getSuit())
                    .append("\n");
        }
        return builder.toString();
    }

    /**
     * Dibuja una cantidad específica de cartas del mazo.
     *
     * @param deck  Lista de cartas del mazo.
     * @param count Número de cartas a extraer.
     * @return Lista de cartas extraídas.
     */
    public static List<Card> drawCards(List<Card> deck, int count) {
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            drawnCards.add(deck.remove(0));
        }
        return drawnCards;
    }



    // Mapa para asociar valores de cartas a números
    private static final Map<String, Integer> cardValues = new HashMap<>();

    static {
        // Asignar valores numéricos a las cartas
        for (int i = 2; i <= 10; i++) {
            cardValues.put(String.valueOf(i), i);
        }
        cardValues.put("jack", 11);
        cardValues.put("queen", 12);
        cardValues.put("king", 13);
        cardValues.put("ace", 14);
    }

    /**
     * Devuelve el valor numérico de una carta.
     *
     * @param cardValue el valor de la carta como String
     * @return el valor numérico de la carta
     * @throws IllegalArgumentException si el valor de la carta no es válido
     */
    public static int getCardValue(String cardValue) {
        Integer value = cardValues.get(cardValue.toLowerCase());
        if (value == null) {
            throw new IllegalArgumentException("Valor de carta no válido: " + cardValue);
        }
        return value;
    }

    /**
     * Compara dos cartas y determina si la primera es mayor o igual que la segunda.
     *
     * @param cardValue1 valor de la primera carta
     * @param cardValue2 valor de la segunda carta
     * @return true si la primera carta es mayor o igual que la segunda
     */
    public static boolean canPlayCard(String cardValue1, String cardValue2) {
        int value1 = getCardValue(cardValue1);
        int value2 = getCardValue(cardValue2);
        return value1 >= value2;
    }


}
