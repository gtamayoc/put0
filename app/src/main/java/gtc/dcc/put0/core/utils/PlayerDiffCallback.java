package gtc.dcc.put0.core.utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import gtc.dcc.put0.core.model.Player;

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

        // Manejar casos null de forma segura
        if (oldPlayer == null || newPlayer == null) {
            return false;
        }

        String oldId = oldPlayer.getId();
        String newId = newPlayer.getId();

        // Si ambos IDs son null, comparar por posición
        if (oldId == null && newId == null) {
            return oldItemPosition == newItemPosition;
        }

        // Si solo uno es null, no son el mismo item
        if (oldId == null || newId == null) {
            return false;
        }

        return oldId.equals(newId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Player oldPlayer = oldPlayers.get(oldItemPosition);
        Player newPlayer = newPlayers.get(newItemPosition);

        // Manejar casos null de forma segura
        if (oldPlayer == null || newPlayer == null) {
            return false;
        }

        // Comparar nombres de forma segura
        String oldName = oldPlayer.getNames();
        String newName = newPlayer.getNames();
        boolean namesEqual = (oldName == null && newName == null) ||
                (oldName != null && oldName.equals(newName));

        return namesEqual &&
                oldPlayer.isCurrentTurn() == newPlayer.isCurrentTurn() &&
                oldPlayer.getCardsCount() == newPlayer.getCardsCount() &&
                oldPlayer.getHand().size() == newPlayer.getHand().size();
    }
}