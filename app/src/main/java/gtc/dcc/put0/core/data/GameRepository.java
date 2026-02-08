package gtc.dcc.put0.core.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import gtc.dcc.put0.core.data.model.CreateRoomRequest;
import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.data.model.JoinRoomRequest;
import gtc.dcc.put0.core.data.model.RoomResponse;
import gtc.dcc.put0.core.data.remote.ApiClient;
import gtc.dcc.put0.core.data.remote.GameWebSocketManager;
import gtc.dcc.put0.core.data.remote.Put0ApiService;
import gtc.dcc.put0.core.model.Card;
import gtc.dcc.put0.core.utils.CoreLogger;
import gtc.dcc.put0.core.local.LocalGameController;
import gtc.dcc.put0.core.local.GameMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepository implements GameWebSocketManager.GameStateListener {
    private static GameRepository instance;

    private final GameWebSocketManager webSocketManager;
    private final Put0ApiService apiService;

    // Local Mode
    private boolean isLocalMode = false;
    private final LocalGameController localGameController;

    // Game State LiveData
    private final MutableLiveData<GameState> _gameState = new MutableLiveData<>();
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    private final MutableLiveData<String> _currentGameId = new MutableLiveData<>();
    private final MutableLiveData<String> _currentPlayerId = new MutableLiveData<>();

    private GameRepository() {
        webSocketManager = new GameWebSocketManager();
        webSocketManager.addListener(this);
        apiService = ApiClient.getService();

        localGameController = new LocalGameController(newState -> {
            _gameState.postValue(newState);
        });
    }

    public static synchronized GameRepository getInstance() {
        if (instance == null) {
            instance = new GameRepository();
        }
        return instance;
    }

    public LiveData<GameState> getGameState() {
        return _gameState;
    }

    public LiveData<String> getError() {
        return _error;
    }

    public LiveData<String> getCurrentGameId() {
        return _currentGameId;
    }

    public LiveData<String> getCurrentPlayerId() {
        return _currentPlayerId;
    }

    // --- REST Actions ---

    public void createGame(String playerName, int botCount, gtc.dcc.put0.core.data.model.MatchMode mode, int deckSize) {
        if (mode == gtc.dcc.put0.core.data.model.MatchMode.SOLO_VS_BOT) {
            CoreLogger.i("Starting local game (Solo vs Bot) with deck size: " + deckSize);
            isLocalMode = true;
            _currentGameId.setValue("local-session");
            _currentPlayerId.setValue("local-human");
            localGameController.startSoloGame(playerName, botCount, deckSize);
            return;
        }

        isLocalMode = false;
        CreateRoomRequest request = new CreateRoomRequest(playerName, botCount, mode, false, 4, deckSize);
        apiService.createRoom(request).enqueue(new Callback<RoomResponse>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RoomResponse room = response.body();
                    updateGameInfo(room);
                } else {
                    _error.setValue("Error creating room: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                _error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void joinGame(String gameId, String playerName) {
        JoinRoomRequest request = new JoinRoomRequest(gameId, playerName);
        apiService.joinRoom(request).enqueue(new Callback<RoomResponse>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RoomResponse room = response.body();
                    updateGameInfo(room);
                } else {
                    _error.setValue("Error joining room: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                _error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void startGame(String gameId) {
        if (gameId == null)
            gameId = _currentGameId.getValue();
        if (gameId == null)
            return;

        apiService.startGame(gameId).enqueue(new Callback<RoomResponse>() {
            @Override
            public void onResponse(Call<RoomResponse> call, Response<RoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _gameState.postValue(response.body().getGameState());
                } else {
                    _error.setValue("Error starting game: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RoomResponse> call, Throwable t) {
                _error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void leaveGame() {
        String gameId = _currentGameId.getValue();
        String playerId = _currentPlayerId.getValue();

        // Execute API call to leave
        if (gameId != null && playerId != null) {
            JoinRoomRequest request = new JoinRoomRequest(gameId, playerId);
            apiService.leaveRoom(gameId, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    CoreLogger.i("Left game room successfully");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    CoreLogger.e("Error leaving room: " + t.getMessage());
                }
            });
        }

        // Always disconnect and clear local state
        webSocketManager.disconnect();
        isLocalMode = false;
        resetGameState();
    }

    public void resetGameState() {
        _gameState.postValue(null);
        _currentGameId.postValue(null);
        _currentPlayerId.postValue(null);
        _error.postValue(null);
        CoreLogger.i("Local game state cleared");
    }

    private void updateGameInfo(RoomResponse room) {
        _currentGameId.setValue(room.getGameId());
        _currentPlayerId.setValue(room.getPlayerId());
        _gameState.setValue(room.getGameState());
        webSocketManager.connect(room.getGameId());
    }

    // --- WebSocket Actions ---

    public void playCard(String playerId, Card card) {
        if (_currentGameId.getValue() == null)
            return;
        CoreLogger.i("[GAME-ACTION] Playing card: " + card + " for player: " + playerId);

        if (isLocalMode) {
            localGameController.playCard(playerId, card);
            return;
        }

        GameAction action = new GameAction(_currentGameId.getValue(), playerId, card);
        webSocketManager.send("/app/game/play", action);
    }

    public void drawCard(String playerId) {
        if (_currentGameId.getValue() == null)
            return;
        CoreLogger.i("[GAME-ACTION] Drawing card for player: " + playerId);

        if (isLocalMode) {
            localGameController.drawCard(playerId);
            return;
        }

        GameAction action = new GameAction(_currentGameId.getValue(), playerId, null);
        webSocketManager.send("/app/game/draw", action);
    }

    public void collectTable(String playerId) {
        if (_currentGameId.getValue() == null)
            return;
        CoreLogger.i("[GAME-ACTION] Collecting table for player: " + playerId);

        if (isLocalMode) {
            localGameController.collectTable(playerId);
            return;
        }

        GameAction action = new GameAction(_currentGameId.getValue(), playerId, null);
        webSocketManager.send("/app/game/collect", action);
    }

    @Override
    public void onGameStateUpdated(GameState gameState) {
        _gameState.postValue(gameState);
    }

    // DTO
    private static class GameAction {
        String gameId;
        String playerId;
        Card card;

        GameAction(String gameId, String playerId, Card card) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.card = card;
        }
    }
}
