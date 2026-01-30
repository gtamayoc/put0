package gtc.dcc.put0.core.utils;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;
import gtc.dcc.put0.core.data.model.Player;

public class PlayerDiffCallback extends DiffUtil.Callback {
    private final List<Player> oldPlayers;
    private final List<Player> newPlayers;

    public PlayerDiffCallback(List<Player> oldPlayers, List<Player> newPlayers) {
        this.oldPlayers = oldPlayers;
        this.newPlayers = newPlayers;
    }

    @Override
    public int getOldListSize() {
        return oldPlayers.size();
    }

    @Override
    public int getNewListSize() {
        return newPlayers.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Player oldPlayer = oldPlayers.get(oldItemPosition);
        Player newPlayer = newPlayers.get(newItemPosition);
        if (oldPlayer == null || newPlayer == null)
            return false;

        String oldId = oldPlayer.getId();
        String newId = newPlayer.getId();
        return oldId != null && oldId.equals(newId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Player oldPlayer = oldPlayers.get(oldItemPosition);
        Player newPlayer = newPlayers.get(newItemPosition);
        if (oldPlayer == null || newPlayer == null)
            return false;

        String oldName = oldPlayer.getName();
        String newName = newPlayer.getName();

        boolean namesEqual = (oldName == null && newName == null) || (oldName != null && oldName.equals(newName));

        return namesEqual && oldPlayer.getCardCount() == newPlayer.getCardCount();
    }
}