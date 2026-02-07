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
import java.util.stream.Collectors;

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

    /**
     * Replaces a card at the specified position with a placeholder.
     * Used for swipe-to-play to maintain the layout space while waiting for a
     * server update.
     */
    public void replaceWithPlaceholder(int position) {
        if (position >= 0 && position < cards.size()) {
            Card placeholder = new Card();
            placeholder.setPlaceholder(true);
            cards.set(position, placeholder);
            notifyItemChanged(position);
        }
    }

    public void updateData(List<Card> newCards, boolean shouldSort) {
        updateData(newCards, shouldSort, false);
    }

    /**
     * Updates the adapter data with a new list of cards, using DiffUtil for smooth
     * animations.
     * 
     * @param newCards     The new list of cards.
     * @param shouldSort   Whether to sort the cards (Phase 1, 2, 3 only).
     * @param deckNotEmpty Whether the main deck still has cards.
     */
    public void updateData(List<Card> newCards, boolean shouldSort, boolean deckNotEmpty) {
        List<Card> cardsToUpdate = new ArrayList<>(newCards);

        // UI UX IMPROVEMENT: If the deck is not empty and we have fewer than 3 cards,
        // add placeholders to avoid layout jumps during replenishment.
        if (deckNotEmpty && cardsToUpdate.size() < 3 && !isPhase4(cardsToUpdate)) {
            while (cardsToUpdate.size() < 3) {
                Card placeholder = new Card();
                placeholder.setPlaceholder(true);
                cardsToUpdate.add(placeholder);
            }
        }

        if (shouldSort) {
            sortCards(cardsToUpdate);
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CardDiffCallback(this.cards, cardsToUpdate));

        this.cards.clear();
        this.cards.addAll(cardsToUpdate);

        // Re-sync selection
        List<Card> currentlySelected = new ArrayList<>(selectedCards);
        selectedCards.clear();
        for (Card card : currentlySelected) {
            if (cards.contains(card)) {
                selectedCards.add(card);
            }
        }

        diffResult.dispatchUpdatesTo(this);

        if (onCardClickListener != null) {
            onCardClickListener.onSelectionChanged(selectedCards);
        }
    }

    private boolean isPhase4(List<Card> list) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return list.stream().anyMatch(Card::isHidden);
        }
        for (Card c : list)
            if (c.isHidden())
                return true;
        return false;
    }

    private void sortCards(List<Card> list) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Sort only non-placeholder cards
            List<Card> sorted = list.stream()
                    .filter(c -> !c.isPlaceholder())
                    .sorted(Comparator.comparingInt(Card::getRankValue))
                    .collect(Collectors.toList());

            List<Card> placeholders = list.stream()
                    .filter(Card::isPlaceholder)
                    .collect(Collectors.toList());

            list.clear();
            list.addAll(sorted);
            list.addAll(placeholders);
        }
    }

    public void shuffleCards() {
        List<Card> shuffledCards = new ArrayList<>(cards);
        Collections.shuffle(shuffledCards);
        updateData(shuffledCards, false, false);
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
        return cards == null ? 0 : cards.size();
    }

    @Override
    public long getItemId(int position) {
        return cards.get(position).hashCode();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cardImage;
        private final View selectionOverlay;

        @SuppressLint("WrongViewCast")
        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handleCardClick(cards.get(position));
                }
            });
        }

        void bind(Card card) {
            itemView.setAlpha(1.0f);
            itemView.setTranslationX(0f);
            itemView.setTranslationY(0f);

            if (card.isPlaceholder()) {
                cardImage.setImageResource(R.drawable.base);
                cardImage.setAlpha(0.4f); // Subtly transparent back
            } else if (isHidden || card.isHidden()) {
                cardImage.setImageResource(R.drawable.base);
                cardImage.setAlpha(1.0f);
            } else {
                int resourceId = DeckUtils.getCardResourceId(itemView.getContext(), card);
                cardImage.setImageResource(resourceId);
                cardImage.setAlpha(1.0f);
            }

            if (selectionOverlay != null) {
                selectionOverlay.setVisibility(selectedCards.contains(card) ? View.VISIBLE : View.GONE);
            }
        }

        private void handleCardClick(Card card) {
            if (card.isPlaceholder())
                return; // Do nothing for placeholders

            if (isPlayerHand && card.isHidden()) {
                card.setHidden(false);
                notifyItemChanged(getAdapterPosition());
                return;
            }

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