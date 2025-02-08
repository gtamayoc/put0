package gtc.dcc.put0.core.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.model.Player;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {
    private List<Player> players;
    private final Context context;
    private final Handler updateHandler = new Handler();
    private final int animationDuration = 300;

    public PlayerAdapter(Context context, List<Player> players) {
        this.context = context;
        this.players = new ArrayList<>(players);
        setHasStableIds(true);
    }

    public void updateData(List<Player> newPlayers) {
        this.players = new ArrayList<>(newPlayers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);

        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.bind(player);

        // Animación al cargar cada ítem
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(-50f);
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(animationDuration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
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
        private final TextView playerName;
        private final LinearLayout hiddenCardsContainer;
        private final LinearLayout visibleCardsContainer;
        private final TextView ronda1Label;
        private final TextView ronda2Label;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.playerName);
            hiddenCardsContainer = itemView.findViewById(R.id.hiddenCardsContainer);
            visibleCardsContainer = itemView.findViewById(R.id.visibleCardsContainer);
            ronda1Label = itemView.findViewById(R.id.ronda_1);
            ronda2Label = itemView.findViewById(R.id.ronda_2);

        }

        void bind(Player player) {
            playerName.setText(player.getNames());

            ronda1Label.setText(String.format(Locale.getDefault(), "Ronda 1: %d cartas", player.getHiddenCards().size()));
            ronda2Label.setText(String.format(Locale.getDefault(), "Ronda 2: %d cartas", player.getVisibleCards().size()));

            populateCards(hiddenCardsContainer, player.getHiddenCards());
            populateCards(visibleCardsContainer, player.getVisibleCards());

        }

        private void animateCardFlip(Card card, CardAdapter adapter,
                                     RecyclerView.ViewHolder holder) {
            if (holder == null) return;

            View cardView = holder.itemView;
            ObjectAnimator flip = ObjectAnimator.ofFloat(cardView,
                    "rotationY", 0f, 180f);
            flip.setDuration(300);
            flip.setInterpolator(new AccelerateDecelerateInterpolator());

            flip.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    cardView.animate()
                            .rotationY(0f)
                            .setStartDelay(1000)
                            .setDuration(300)
                            .start();
                }
            });

            flip.start();
        }

        private void populateCards(LinearLayout container, List<Card> cards) {
            container.removeAllViews(); // Limpia cualquier vista previa en el contenedor
            for (Card card : cards) {
                View cardView = LayoutInflater.from(context).inflate(R.layout.item_card, container, false);
                ImageView cardImage = cardView.findViewById(R.id.cardImage);
                cardImage.setImageResource(card.getResourceId()); // Usa tu lógica para asignar imágenes
                container.addView(cardView);
            }
        }

    }

    private static class CardSpacingItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position != 0) {
                outRect.left = -30;
            }
        }
    }
}