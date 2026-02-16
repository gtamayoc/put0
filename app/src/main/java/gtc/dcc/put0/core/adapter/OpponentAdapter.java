package gtc.dcc.put0.core.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import gtc.dcc.put0.R;
import gtc.dcc.put0.core.data.model.Player;
import java.util.ArrayList;
import java.util.List;

public class OpponentAdapter extends RecyclerView.Adapter<OpponentAdapter.OpponentViewHolder> {

    private final List<Player> opponents = new ArrayList<>();
    private final Context context;
    private String currentTurnId;

    public OpponentAdapter(Context context) {
        this.context = context;
    }

    public void updateData(List<Player> newOpponents, String currentTurnId) {
        this.currentTurnId = currentTurnId;
        this.opponents.clear();
        if (newOpponents != null) {
            this.opponents.addAll(newOpponents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OpponentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_opponent_compact, parent, false);
        return new OpponentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpponentViewHolder holder, int position) {
        holder.bind(opponents.get(position));
    }

    @Override
    public int getItemCount() {
        return opponents.size();
    }

    class OpponentViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvCards;
        private final TextView tvName;

        public OpponentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivOpponent);
            tvCards = itemView.findViewById(R.id.tvOpponentCards);
            tvName = itemView.findViewById(R.id.tvOpponentName);
        }

        void bind(Player player) {
            tvName.setText(player.getName());

            // Hybrid Logic: Prefer real list size if available (Local Game), else calculate
            int handSize;
            if (player.getHand() != null && !player.getHand().isEmpty()) {
                handSize = player.getHand().size();
            } else {
                int total = player.getCardCount();
                int visible = player.getVisibleCards() != null ? player.getVisibleCards().size() : 0;
                int hidden = player.getHiddenCards() != null ? player.getHiddenCards().size() : 0;
                handSize = Math.max(0, total - visible - hidden);
            }

            tvCards.setText(String.valueOf(handSize));

            // Highlight Active Turn
            boolean isTurn = currentTurnId != null && currentTurnId.equals(player.getId());
            ivAvatar.setAlpha(isTurn ? 1.0f : 0.6f);

            if (isTurn) {
                ivAvatar.setBorderColor(context.getResources().getColor(R.color.md_primary)); // Active Border
                ivAvatar.setBorderWidth(4); // Thicker border
            } else {
                ivAvatar.setBorderColor(0xAAFFFFFF); // Default
                ivAvatar.setBorderWidth(2);
            }
        }
    }
}
