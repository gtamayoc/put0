package gtc.dcc.put0.core.utils;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;
import gtc.dcc.put0.core.model.Card;

public class CardDiffCallback extends DiffUtil.Callback {
    private final List<Card> oldList;
    private final List<Card> newList;
    private final List<Card> selectedCards;

    public CardDiffCallback(List<Card> oldList, List<Card> newList, List<Card> selectedCards) {
        this.oldList = oldList;
        this.newList = newList;
        this.selectedCards = selectedCards;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Card oldCard = oldList.get(oldItemPosition);
        Card newCard = newList.get(newItemPosition);

        // Now that Card has a unique instanceId and equals uses it,
        // we can simply use equals for item identity.
        return oldCard.equals(newCard);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Card oldCard = oldList.get(oldItemPosition);
        Card newCard = newList.get(newItemPosition);

        // Check unique identity first (extra safe)
        if (!oldCard.equals(newCard)) {
            return false;
        }

        // Check other fields that might change but keep same instanceId
        // Such as selection status, hidden status, etc.
        boolean sameSelection = (selectedCards != null) &&
                (selectedCards.contains(oldCard) == selectedCards.contains(newCard));

        return oldCard.isHidden() == newCard.isHidden() &&
                oldCard.isPlaceholder() == newCard.isPlaceholder() &&
                oldCard.getRankValue() == newCard.getRankValue() &&
                oldCard.getSuit() == newCard.getSuit() &&
                sameSelection;
    }
}
