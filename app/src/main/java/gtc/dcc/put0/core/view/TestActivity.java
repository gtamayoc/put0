package gtc.dcc.put0.core.view;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.adapter.CardAdapter;
import gtc.dcc.put0.core.model.Card;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Crear una lista de cartas directamente
        List<Card> testCards = new ArrayList<>();
        testCards.add(new Card("back", "base", R.drawable.base));

        // Ases
        testCards.add(new Card("clubs", "A", R.drawable.card_ace_clubs));
        testCards.add(new Card("diamonds", "A", R.drawable.card_ace_diamonds));
        testCards.add(new Card("hearts", "A", R.drawable.card_ace_hearts));
        testCards.add(new Card("spades", "A", R.drawable.card_ace_spades));

        // Cartas numeradas
        for (int i = 2; i <= 10; i++) {
            testCards.add(new Card("clubs", String.valueOf(i),
                    this.getResources().getIdentifier("card_" + i + "_clubs", "drawable", this.getPackageName())));
            testCards.add(new Card("diamonds", String.valueOf(i),
                    this.getResources().getIdentifier("card_" + i + "_diamonds", "drawable", this.getPackageName())));
            testCards.add(new Card("hearts", String.valueOf(i),
                    this.getResources().getIdentifier("card_" + i + "_hearts", "drawable", this.getPackageName())));
            testCards.add(new Card("spades", String.valueOf(i),
                    this.getResources().getIdentifier("card_" + i + "_spades", "drawable", this.getPackageName())));
        }

        // Figuras
        testCards.add(new Card("clubs", "J", R.drawable.card_jack_clubs));
        testCards.add(new Card("diamonds", "J", R.drawable.card_jack_diamonds));
        testCards.add(new Card("hearts", "J", R.drawable.card_jack_hearts));
        testCards.add(new Card("spades", "J", R.drawable.card_jack_spades));

        testCards.add(new Card("clubs", "Q", R.drawable.card_queen_clubs));
        testCards.add(new Card("diamonds", "Q", R.drawable.card_queen_diamonds));
        testCards.add(new Card("hearts", "Q", R.drawable.card_queen_hearts));
        testCards.add(new Card("spades", "Q", R.drawable.card_queen_spades));

        testCards.add(new Card("clubs", "K", R.drawable.card_king_clubs));
        testCards.add(new Card("diamonds", "K", R.drawable.card_king_diamonds));
        testCards.add(new Card("hearts", "K", R.drawable.card_king_hearts));
        testCards.add(new Card("spades", "K", R.drawable.card_king_spades));

        // Configurar el RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTest);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        CardAdapter adapter = new CardAdapter(testCards, true, false);
        recyclerView.setAdapter(adapter);

    }
}