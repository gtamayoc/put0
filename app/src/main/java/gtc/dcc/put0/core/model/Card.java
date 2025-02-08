package gtc.dcc.put0.core.model;

import java.util.Objects;

public class Card {
    private String suit; // Ejemplo: "Corazones", "Diamantes"
    private String value; // Ejemplo: "As", "2", ..., "Rey"
    private String imagePath; // Ruta a la imagen de la carta (opcional)
    private int resourceId; // ID del recurso visual de la carta
    private boolean faceUp;

    public Card(String suit, String value, int resourceId) {
        this.suit = suit;
        this.value = value;
        this.imagePath = imagePath;
        this.resourceId = resourceId;
        this.faceUp = false;
    }

    // Getters
    public String getSuit() {
        return suit;
    }

    public String getValue() {
        return value;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return value.equals(card.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}