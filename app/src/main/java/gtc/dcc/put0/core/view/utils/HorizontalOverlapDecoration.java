package gtc.dcc.put0.core.view.utils;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalOverlapDecoration extends RecyclerView.ItemDecoration {
    private final int overlapAmount;

    public HorizontalOverlapDecoration(int overlapAmount) {
        this.overlapAmount = overlapAmount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);

        // No overlap for the first item
        if (position == 0) {
            return;
        }

        // Negative margin to pull items left
        outRect.left = -overlapAmount;
    }
}
