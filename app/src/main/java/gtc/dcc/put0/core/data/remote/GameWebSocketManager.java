package gtc.dcc.put0.core.data.remote;

import gtc.dcc.put0.core.utils.CoreLogger;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import gtc.dcc.put0.core.data.model.GameState;

public class GameWebSocketManager {
    private static final String TAG = "GameWebSocketManager";
    // private static final String WS_URL = "ws://10.0.2.2:8080/ws/websocket"; //
    // Removed hardcoded URL

    private StompClient mStompClient;
    private CompositeDisposable compositeDisposable;
    private final Gson gson = new Gson();
    private final List<GameStateListener> listeners = new ArrayList<>();

    public interface GameStateListener {
        void onGameStateUpdated(GameState gameState);
    }

    public void addListener(GameStateListener listener) {
        listeners.add(listener);
    }

    public void connect(String gameId) {
        if (mStompClient != null && mStompClient.isConnected()) {
            return;
        }

        String baseUrl = ApiClient.getBaseUrl(); // e.g., http://192.168.1.5:8080/
        String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
        wsUrl = wsUrl + "ws/websocket";

        CoreLogger.d("Connecting to WebSocket: " + wsUrl);

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        compositeDisposable = new CompositeDisposable();

        // Lifestyle events
        Disposable lifecycle = mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            CoreLogger.d("Stomp connection opened");
                            subscribeToGame(gameId);
                            break;
                        case ERROR:
                            CoreLogger.e(lifecycleEvent.getException(), "Stomp connection error");
                            break;
                        case CLOSED:
                            CoreLogger.d("Stomp connection closed");
                            break;
                    }
                });
        compositeDisposable.add(lifecycle);

        mStompClient.connect();
    }

    private void subscribeToGame(String gameId) {
        Disposable topic = mStompClient.topic("/topic/game/" + gameId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    CoreLogger.d("Received " + topicMessage.getPayload());

                    // Parse the wrapper object which likely contains "gameState" field
                    // But wait, server sends GameStateUpdate object which has 'gameState' field.
                    // Let's parse strictly.
                    try {
                        // Assuming simple parsing for now, user might need DTO for update
                        GameStateUpdate update = gson.fromJson(topicMessage.getPayload(), GameStateUpdate.class);
                        if (update != null && update.gameState != null) {
                            notifyListeners(update.gameState);
                        }
                    } catch (Exception e) {
                        CoreLogger.e(e, "Error parsing message");
                    }
                }, throwable -> {
                    CoreLogger.e(throwable, "Error on subscribe");
                });
        compositeDisposable.add(topic);
    }

    public void send(String endpoint, Object payload) {
        if (mStompClient != null && mStompClient.isConnected()) {
            compositeDisposable.add(mStompClient.send(endpoint, gson.toJson(payload))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        CoreLogger.d("Sent message to " + endpoint);
                    }, throwable -> {
                        CoreLogger.e(throwable, "Error sending message");
                    }));
        }
    }

    private void notifyListeners(GameState gameState) {
        for (GameStateListener listener : listeners) {
            listener.onGameStateUpdated(gameState);
        }
    }

    public void disconnect() {
        if (mStompClient != null) {
            mStompClient.disconnect();
        }
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    // Inner class for Update DTO matching Server's GameStateUpdate
    private static class GameStateUpdate {
        public GameState gameState;
        public String message;
        public String type;
    }
}
