package gtc.dcc.put0.core.adapter;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.utils.CardDiffCallback;
import gtc.dcc.put0.core.utils.DeckUtils;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private List<Card> cards;
    private final boolean isPlayerHand;
    private final boolean isHidden;
    private OnCardClickListener onCardClickListener;
    private boolean multipleSelectionEnabled = false;
    private int maxSelectableCards = 3;
    private final List<Card> selectedCards = new ArrayList<>();

    public interface OnCardClickListener {
        void onCardClick(Card card, boolean isCurrentlyHidden);
        void onSelectionChanged(List<Card> selectedCards);
    }

    public CardAdapter(List<Card> cards, boolean isPlayerHand, boolean isHidden) {
        this.cards = new ArrayList<>(cards);
        this.isPlayerHand = isPlayerHand;
        this.isHidden = isHidden;
        setHasStableIds(true);
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        this.onCardClickListener = listener;
    }

    public void setMultipleSelectionEnabled(boolean enabled) {
        this.multipleSelectionEnabled = enabled;
        if (!enabled) {
            clearSelection();
        }
    }

    public void setMaxSelectableCards(int max) {
        this.maxSelectableCards = max;
    }

    public void clearSelection() {
        selectedCards.clear();
        notifyDataSetChanged();
        if (onCardClickListener != null) {
            onCardClickListener.onSelectionChanged(new ArrayList<>());
        }
    }

    public List<Card> getSelectedCards() {
        return new ArrayList<>(selectedCards);
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public void updateData(List<Card> newCards, boolean shouldSort) {
        List<Card> cardsToUpdate = new ArrayList<>(newCards);

        if (shouldSort) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(cardsToUpdate, Comparator.comparingInt(card -> DeckUtils.getCardValue(card.getValue())));
            }
        }

        // Guardar selecciones actuales
        List<Card> currentlySelected = new ArrayList<>(selectedCards);

        // Usar DiffUtil para mejorar el rendimiento
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CardDiffCallback(this.cards, cardsToUpdate));

        // Actualizar datos
        this.cards.clear();
        this.cards.addAll(cardsToUpdate);

        // Restaurar selecciones si las cartas aún existen
        selectedCards.clear();
        for (Card card : currentlySelected) {
            if (cards.contains(card)) {
                selectedCards.add(card);
            }
        }

        diffResult.dispatchUpdatesTo(this);

        // Notificar cambios en la selección
        if (onCardClickListener != null) {
            onCardClickListener.onSelectionChanged(selectedCards);
        }
    }

    // Añadir método para ordenamiento aleatorio
    public void shuffleCards() {
        List<Card> shuffledCards = new ArrayList<>(cards);
        Collections.shuffle(shuffledCards);
        updateData(shuffledCards, false);
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.bind(card);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    @Override
    public long getItemId(int position) {
        return cards.get(position).hashCode();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cardImage;

        @SuppressLint("WrongViewCast")
        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handleCardClick(cards.get(position));
                }
            });
        }

        void bind(Card card) {
            // Establecer la imagen de la carta
            if (isHidden) {
                cardImage.setImageResource(R.drawable.base);
            } else {
                cardImage.setImageResource(card.getResourceId());
            }

            // Aplicar estado seleccionado/no seleccionado
            itemView.setSelected(selectedCards.contains(card));
        }

        private void handleCardClick(Card card) {
            if (multipleSelectionEnabled && isPlayerHand) {
                if (selectedCards.contains(card)) {
                    selectedCards.remove(card);
                } else if (selectedCards.size() < maxSelectableCards) {
                    selectedCards.add(card);
                }
                notifyDataSetChanged();
                if (onCardClickListener != null) {
                    onCardClickListener.onSelectionChanged(selectedCards);
                }
            } else if (onCardClickListener != null) {
                onCardClickListener.onCardClick(card, isHidden);
            }
        }
    }
}