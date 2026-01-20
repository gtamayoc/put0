package gtc.dcc.put0.core.model;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Stack;
import java.util.List;

public class Deck {
    private final Stack<Card> cards;

    public Deck() {
        this.cards = new Stack<>();
    }

    public void addCard(Card card) {
        cards.push(card);
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.pop();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public void clear() {
        cards.clear();
    }

    public List<Card> getCards() {
        return Lists.newArrayList(cards);
    }
}
