package gtc.dcc.put0.core.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.data.model.Player;
import gtc.dcc.put0.core.utils.PlayerDiffCallback;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerViewHolder> {
    private List<Player> players;
    private final Context context;
    private static final int ANIMATION_DURATION = 300;
    private OnPlayerClickListener onPlayerClickListener;
    private boolean multipleSelectionEnabled = false;
    private int maxSelectablePlayers = 3;
    private final List<Player> selectedPlayers = new ArrayList<>();
    private String currentPlayerId;

    public interface OnPlayerClickListener {
        void onPlayerClick(Player player);

        void onSelectionChanged(List<Player> selectedPlayers);
    }

    public PlayerListAdapter(Context context, List<Player> players) {
        this.context = context;
        this.players = new ArrayList<>(players);
        setHasStableIds(true);
    }

    public void setOnPlayerClickListener(OnPlayerClickListener listener) {
        this.onPlayerClickListener = listener;
    }

    public void setCurrentPlayerId(String playerId) {
        this.currentPlayerId = playerId;
        notifyDataSetChanged();
    }

    public void setMultipleSelectionEnabled(boolean enabled) {
        this.multipleSelectionEnabled = enabled;
        if (!enabled) {
            clearSelection();
        }
    }

    public void setMaxSelectablePlayers(int max) {
        this.maxSelectablePlayers = max;
    }

    public void clearSelection() {
        selectedPlayers.clear();
        notifyDataSetChanged();
        if (onPlayerClickListener != null) {
            onPlayerClickListener.onSelectionChanged(new ArrayList<>());
        }
    }

    public List<Player> getSelectedPlayers() {
        return new ArrayList<>(selectedPlayers);
    }

    public void updateData(List<Player> newPlayers) {
        if (newPlayers == null)
            return;
        List<Player> newList = new ArrayList<>(newPlayers);
        List<Player> oldList = new ArrayList<>(this.players);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PlayerDiffCallback(oldList, newList), true);
        this.players.clear();
        this.players.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_list_item, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.bind(player);
        if (holder.itemView.getAlpha() != 1f) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(-50f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION)
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    @Override
    public long getItemId(int position) {
        return players.get(position).hashCode();
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder {
        private final ImageView userImage;
        private final TextView userName;
        private final TextView userStatus;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            userStatus = itemView.findViewById(R.id.userStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    handlePlayerClick(players.get(position));
                }
            });
        }

        void bind(Player player) {
            userName.setText(player.getName());

            boolean isTurn = currentPlayerId != null && currentPlayerId.equals(player.getId());
            String statusText = String.format("%s | Cartas: %d",
                    isTurn ? "En turno" : "Esperando",
                    player.getCardCount());
            userStatus.setText(statusText);

            // Actualizar colores y estados
            userStatus
                    .setTextColor(isTurn ? context.getColor(R.color.btns) : context.getColor(R.color.backgroundLight));

            itemView.invalidate();
        }

        private void handlePlayerClick(Player player) {
            if (multipleSelectionEnabled) {
                if (selectedPlayers.contains(player)) {
                    selectedPlayers.remove(player);
                } else if (selectedPlayers.size() < maxSelectablePlayers) {
                    selectedPlayers.add(player);
                }
                notifyDataSetChanged();
                if (onPlayerClickListener != null) {
                    onPlayerClickListener.onSelectionChanged(selectedPlayers);
                }
            } else if (onPlayerClickListener != null) {
                onPlayerClickListener.onPlayerClick(player);
            }
        }
    }
}