package gtc.dcc.put0.core.utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import gtc.dcc.put0.core.model.Card;

public class CardDiffCallback extends DiffUtil.Callback {
    private final List<Card> oldList;
    private final List<Card> newList;

    public CardDiffCallback(List<Card> oldList, List<Card> newList) {
        this.oldList = oldList;
        this.newList = newList;
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
        // Comparar por una ID única o referencia de objeto
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Comparar el contenido de las cartas
        return oldList.get(oldItemPosition).getValue().equals(newList.get(newItemPosition).getValue());
    }
}
