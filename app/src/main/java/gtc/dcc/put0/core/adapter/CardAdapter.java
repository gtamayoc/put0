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
        // setHasStableIds(true); // Disable to avoid potential collisions if duplicates
        // exist
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
                // Use a proper comparator if available, for now sort by value
                Collections.sort(cardsToUpdate, Comparator.comparingInt(Card::getRankValue));
            }
        }

        // DiffUtil can be tricky with mutable/immutable mixed checking.
        // For stability in this verification phase, we use notifyDataSetChanged.
        this.cards.clear();
        this.cards.addAll(cardsToUpdate);

        List<Card> currentlySelected = new ArrayList<>(selectedCards);
        selectedCards.clear();
        for (Card card : currentlySelected) {
            if (cards.contains(card)) {
                selectedCards.add(card);
            }
        }

        notifyDataSetChanged();

        /*
         * DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new
         * CardDiffCallback(this.cards, cardsToUpdate));
         * this.cards.clear();
         * this.cards.addAll(cardsToUpdate);
         * ...
         * diffResult.dispatchUpdatesTo(this);
         */

        if (onCardClickListener != null) {
            onCardClickListener.onSelectionChanged(selectedCards);
        }
    }

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
        gtc.dcc.put0.core.utils.CoreLogger.d("CardAdapter: Binding card at " + position + ": " + card);
        holder.bind(card);
    }

    @Override
    public int getItemCount() {
        if (cards == null)
            return 0;
        // gtc.dcc.put0.core.utils.CoreLogger.d("CardAdapter: Item count: " +
        // cards.size()); // Too spammy?
        return cards.size();
    }

    @Override
    public long getItemId(int position) {
        return cards.get(position).hashCode();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cardImage;
        private final View selectionOverlay; // New field

        @SuppressLint("WrongViewCast")
        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay); // Initialize

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handleCardClick(cards.get(position));
                }
            });
        }

        void bind(Card card) {
            if (isHidden) {
                cardImage.setImageResource(R.drawable.base);
            } else {
                int resourceId = DeckUtils.getCardResourceId(itemView.getContext(), card);
                cardImage.setImageResource(resourceId);
            }
            // Update visibility of the overlay based on selection
            if (selectionOverlay != null) {
                selectionOverlay.setVisibility(selectedCards.contains(card) ? View.VISIBLE : View.GONE);
            }
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