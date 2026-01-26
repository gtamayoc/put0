package gtc.dcc.put0.core.viewmodel;

import gtc.dcc.put0.core.data.GameRepository;
import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.utils.CoreLogger;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import gtc.dcc.put0.core.engine.GameEvent;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import gtc.dcc.put0.core.model.Card;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameViewModel extends ViewModel {
    private static final String TAG = "GameViewModel";

    private final GameRepository repository;

    public GameViewModel() {
        repository = GameRepository.getInstance();
    }

    // --- State ---

    public LiveData<GameState> getGameState() {
        return repository.getGameState();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public LiveData<String> getCurrentGameId() {
        return repository.getCurrentGameId();
    }

    public LiveData<String> getCurrentPlayerId() {
        return repository.getCurrentPlayerId();
    }

    // RxJava Event Stream (Assuming we want to keep it or move it to Repo?
    // For now, let's keep it simple. If events come via Repo, we can expose them.
    // The previous implementation had a local Subject.
    // We can add getGameEvents() to Repository if needed, but for now I'll just
    // expose a simple stream or empty if unused.
    // Actually, let's just create a pass-through if needed, but the logs didn't
    // show heavy usage.
    // I'll add a placeholder or simple Subject if logic demands it.)
    private final PublishSubject<GameEvent> _gameEvents = PublishSubject.create();

    public Observable<GameEvent> getGameEvents() {
        return _gameEvents;
    }

    // --- REST Actions ---

    public void createGame(String playerName, int botCount, gtc.dcc.put0.core.data.model.MatchMode mode) {
        repository.createGame(playerName, botCount, mode);
    }

    public void joinGame(String gameId, String playerName) {
        repository.joinGame(gameId, playerName);
    }

    public void startGame(String gameId) {
        repository.startGame(gameId);
    }

    public void leaveGame() {
        repository.leaveGame();
    }

    // --- WebSocket Actions ---

    public void playCard(String playerId, Card card) {
        repository.playCard(playerId, card);
    }

    public void drawCard(String playerId) {
        repository.drawCard(playerId);
    }

    public void collectTable(String playerId) {
        repository.collectTable(playerId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Do NOT disconnect repository here because ViewModel is destroyed on Activity
        // rotation or change,
        // but we want persistence. Repository handles its own lifecycle or manual
        // disconnect.
    }
}