package gtc.dcc.put0.core.model;

import java.util.ArrayList;
import java.util.List;

import gtc.dcc.put0.core.utils.DeckUtils;

public class Player extends User {
    private List<Card> hiddenCards;
    private List<Card> visibleCards;
    private List<Card> hand;
    private int score; // Nuevo: para llevar el puntaje
    private boolean isCurrentTurn; // Nuevo: para controlar el turno
    private Integer currentTurn; // Nuevo: para controlar el turno

    public Player() {
    }

    // En el constructor de Player o donde inicialices los jugadores
    public Player(String name) {
        super(name);
        this.hand = new ArrayList<>();
        this.visibleCards = new ArrayList<>();
        this.hiddenCards = new ArrayList<>();
        this.isCurrentTurn = false;  // Asegurarse que esto está inicializado correctamente
    }

    // Constructor con nombre
    public Player(String name, int demo) {
        super(name);
        this.hand = new ArrayList<>();
        this.visibleCards = new ArrayList<>();
        this.hiddenCards = new ArrayList<>();
    }

    public Player(String id, String emailAddress, String userPassword, String names, String surNames, String userName, String state, Rol rol) {
        super(id, emailAddress, userPassword, names, surNames, userName, state, rol);
    }

    // Nuevos métodos
    public int getCardsCount() {
        return hand.size() + visibleCards.size() + hiddenCards.size();
    }

    public boolean canPlayCard(Card card, Card topCard) {
        return topCard == null || DeckUtils.getCardValue(card.getValue()) >= DeckUtils.getCardValue(topCard.getValue());
    }

    public void playCard(Card card, List<Card> tableCards) {
        if (hand.contains(card)) {
            hand.remove(card);
            tableCards.add(card);
        }
    }

    public List<Card> getHiddenCards() {
        return hiddenCards;
    }

    public void setHiddenCards(List<Card> hiddenCards) {
        this.hiddenCards = hiddenCards;
    }

    public void addHiddenCards(Card card) {
        hiddenCards.add(card);
    }

    public List<Card> getVisibleCards() {
        return visibleCards;
    }

    public void setVisibleCards(List<Card> visibleCards) {
        this.visibleCards = visibleCards;
    }

    public void addVisibleCards(Card card) {
        visibleCards.add(card);
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public boolean hasCardsInHand() {
        return !hand.isEmpty();
    }

    public Integer getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Integer currentTurn) {
        this.currentTurn = currentTurn;
    }

    // En la clase Player, asegúrate que el setter funciona correctamente
    public void setIsCurrentTurn(boolean isCurrentTurn) {
        this.isCurrentTurn = isCurrentTurn;
    }

    public boolean isCurrentTurn() {
        return this.isCurrentTurn;
    }

}
