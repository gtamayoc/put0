package gtc.dcc.put0.core.view.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import gtc.dcc.put0.core.adapter.CardAdapter;

public class CardTouchHelper extends ItemTouchHelper.Callback {

    private final CardAdapter adapter;
    private final OnSwipeListener listener;

    public interface OnSwipeListener {
        void onSwipeUp(int position);
    }

    public CardTouchHelper(CardAdapter adapter, OnSwipeListener listener) {
        this.adapter = adapter;
        this.listener = listener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Allow swipe up to play card
        int dragFlags = 0; // ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT; // If we want reordering
        int swipeFlags = ItemTouchHelper.UP;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        return false; // Drag and drop not strictly required for "throwing", but could be added later
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.UP) {
            listener.onSwipeUp(viewHolder.getAdapterPosition());
        }
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }
}
