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
        if (selectedCards.isEmpty())
            return;

        List<Card> previouslySelected = new ArrayList<>(selectedCards);
        selectedCards.clear();

        // Notify only the items that were previously selected
        for (int i = 0; i < cards.size(); i++) {
            if (previouslySelected.contains(cards.get(i))) {
                notifyItemChanged(i, "SELECTION");
            }
        }

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
     * Replaces the played card with an invisible placeholder.
     * This keeps the layout stable (no shrinking) until the new card arrives.
     */
    public void markCardAsPlayed(int position) {
        if (position >= 0 && position < cards.size()) {
            Card placeholder = new Card();
            placeholder.setPlaceholder(true);
            cards.set(position, placeholder);
            notifyItemChanged(position);
        }
    }

    public void removeCardAt(int position) {
        if (position >= 0 && position < cards.size()) {
            cards.remove(position);
            notifyItemRemoved(position);
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

        // UI UX IMPROVEMENT: Placeholder logic removed as per user request to avoid
        // "ghost" base cards.
        // The RecyclerView will simply shrink or show the actual number of cards.

        if (shouldSort) {
            sortCards(cardsToUpdate);
        }

        DiffUtil.DiffResult diffResult = DiffUtil
                .calculateDiff(new CardDiffCallback(this.cards, cardsToUpdate, selectedCards));

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
        holder.bind(cards.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if (payload.equals("SELECTION")) {
                    boolean isSelected = selectedCards.contains(cards.get(position));
                    holder.updateSelection(isSelected, true);
                    return; // Stop after handling selection
                }
            }
        }
        // If no payload or unrecognized, do full bind
        super.onBindViewHolder(holder, position, payloads);
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
        private final View cardContent;
        private final View selectionOverlay;

        @SuppressLint("WrongViewCast")
        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);
            cardContent = itemView.findViewById(R.id.cardContainer);
            selectionOverlay = null; // No longer used

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handleCardClick(cards.get(position));
                }
            });
        }

        void bind(Card card) {
            itemView.setAlpha(1.0f);
            // Translation/Scale/Elevation reset handled below based on selection

            if (card.isPlaceholder()) {
                // INVISIBLE PLACEHOLDER: Keep separation but don't show anything
                cardImage.setImageDrawable(null);
                itemView.setAlpha(0f); // Make the whole container invisible but taking space
            } else if (isHidden || (card.isHidden() && !isPlayerHand)) {
                cardImage.setImageResource(R.drawable.base);
                cardImage.setAlpha(1.0f);
                itemView.setAlpha(1.0f);
            } else {
                // Sanity check: If card has invalid rank, don't show base, show nothing
                if (card.getRankValue() <= 0) {
                    cardImage.setImageDrawable(null);
                    return;
                }

                int resourceId = DeckUtils.getCardResourceId(itemView.getContext(), card);
                cardImage.setImageResource(resourceId);
                cardImage.setAlpha(1.0f);
                itemView.setAlpha(1.0f);
            }

            // Handle Selection Visuals
            updateSelection(!card.isPlaceholder() && selectedCards.contains(card), false);
        }

        void updateSelection(boolean isSelected, boolean animate) {
            float density = itemView.getResources().getDisplayMetrics().density;
            float targetY = isSelected ? -12f * density : 0f;
            float targetScale = isSelected ? 1.08f : 1.0f;
            float targetElevation = isSelected ? 16f * density : 0f;

            if (cardContent instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) cardContent;
                cardView.setStrokeWidth(0); // Border removed as requested

                if (animate) {
                    cardContent.animate().cancel();
                    cardContent.animate()
                            .translationY(targetY)
                            .scaleX(targetScale)
                            .scaleY(targetScale)
                            .translationZ(targetElevation)
                            .setDuration(150)
                            .start();
                } else {
                    cardContent.setTranslationY(targetY);
                    cardContent.setScaleX(targetScale);
                    cardContent.setScaleY(targetScale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cardContent.setTranslationZ(targetElevation);
                    }
                }
            }
        }

        private void handleCardClick(Card card) {
            if (card.isPlaceholder())
                return; // Do nothing for placeholders

            if (isPlayerHand && card.isHidden()) {
                card.setHidden(false);
                notifyItemChanged(getAdapterPosition(), "SELECTION"); // Or full update if image changes
                return;
            }

            if (multipleSelectionEnabled && isPlayerHand) {
                int position = getAdapterPosition();
                if (selectedCards.contains(card)) {
                    selectedCards.remove(card);
                } else {
                    // Check duplicate constraint
                    if (!selectedCards.isEmpty()) {
                        int currentRank = selectedCards.get(0).getRankValue();
                        if (card.getRankValue() != currentRank) {
                            // UX Decide: Switch to new rank group
                            clearSelection(); // This notifies changes
                            // selectedCards is now empty, add the new one
                        }
                    }

                    if (selectedCards.size() < maxSelectableCards) {
                        selectedCards.add(card);
                    }
                }

                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position, "SELECTION");
                }

                if (onCardClickListener != null) {
                    onCardClickListener.onSelectionChanged(selectedCards);
                }
            } else if (onCardClickListener != null) {
                onCardClickListener.onCardClick(card, isHidden);
            }
        }
    }
}