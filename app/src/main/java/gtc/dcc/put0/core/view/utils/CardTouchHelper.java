package gtc.dcc.put0.core.view.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import gtc.dcc.put0.core.adapter.CardAdapter;
import gtc.dcc.put0.core.model.Card;

public class CardTouchHelper extends ItemTouchHelper.Callback {

    private final CardAdapter adapter;
    private final OnSwipeListener listener;

    public interface OnSwipeListener {
        void onSwipeUp(int position, Card card);
    }

    public CardTouchHelper(CardAdapter adapter, OnSwipeListener listener) {
        this.adapter = adapter;
        this.listener = listener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position < adapter.getCards().size()) {
            Card card = adapter.getCards().get(position);
            // Rule: "si no ha descubierto la carta no se puede lanzar"
            // Disable swipe if the card is still hidden
            if (card.isHidden()) {
                return makeMovementFlags(0, 0);
            }
        }

        // Allow swipe up to play card
        int dragFlags = 0;
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
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < adapter.getCards().size()) {
                // Get the card BEFORE removing it from the adapter
                Card card = adapter.getCards().get(position);

                // Replace the card in the adapter with a placeholder IMMEDIATELY
                // This maintains the layout space while the play request is processed
                adapter.replaceWithPlaceholder(position);

                // Then trigger the actual play logic with the card we cached
                listener.onSwipeUp(position, card);
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Reset view properties to ensure cards don't stay invisible/translated
        // This is called when ItemTouchHelper releases the view (swipe completes or is
        // cancelled)
        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.setTranslationX(0f);
        viewHolder.itemView.setTranslationY(0f);
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
