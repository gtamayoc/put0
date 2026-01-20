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
import gtc.dcc.put0.core.model.Rank;
import gtc.dcc.put0.core.model.Suit;

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

        // Iterate through all Suits and Ranks
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                testCards.add(new Card(suit, rank));
            }
        }

        // Configurar el RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTest);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        CardAdapter adapter = new CardAdapter(testCards, true, false);
        recyclerView.setAdapter(adapter);

    }
}