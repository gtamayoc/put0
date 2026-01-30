# PUT0 Android - Estado Esperado

## 🎯 Objetivo Final

Aplicación Android completamente funcional que:
- ✅ Se conecta al servidor Spring Boot
- ✅ Sincroniza en tiempo real vía WebSocket
- ✅ Implementa todas las reglas del juego PUT0
- ✅ Soporta modo Solo (con bots) y Multijugador
- ✅ UI/UX fluida y responsive
- ✅ Manejo robusto de errores de red

---

## 1. Dependencias Completas

### build.gradle.kts
```kotlin
dependencies {
    // Existentes
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.firebase.bom)
    implementation(libs.gson)
    
    // NUEVAS: Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // NUEVAS: WebSocket
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    
    // NUEVAS: Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

---

## 2. Modelos de Datos Completos

### Card.java (Actualizado)
```java
public class Card {
    private String suit;      // "Corazones", "Diamantes", etc.
    private String value;     // "As", "2", "Rey", etc.
    private int resourceId;
    private boolean faceUp;
    
    // NUEVO: Conversión a DTO
    public CardDTO toDTO() {
        return new CardDTO(
            convertToNumericValue(value),
            convertToBackendSuit(suit)
        );
    }
    
    // NUEVO: Conversión desde DTO
    public static Card fromDTO(CardDTO dto, Context context) {
        String androidValue = convertFromNumeric(dto.getValue());
        String androidSuit = convertFromBackendSuit(dto.getSuit());
        int resourceId = getResourceId(context, androidValue, androidSuit);
        return new Card(androidSuit, androidValue, resourceId);
    }
}
```

### Player.java (Actualizado)
```java
public class Player extends User {
    private String playerId;           // NUEVO: ID del servidor
    private List<Card> hand;           // Máximo 3 cartas
    private List<Card> hiddenCards;    // 3 cartas ocultas
    private List<Card> visibleCards;   // 3 cartas visibles
    private boolean isBot;             // NUEVO
    private boolean isCurrentTurn;
    
    // NUEVO: Conversión desde DTO
    public static Player fromDTO(PlayerDTO dto, Context context) {
        Player player = new Player(dto.getName());
        player.setPlayerId(dto.getId());
        player.setBot(dto.isBot());
        
        // Convertir cartas
        for (CardDTO cardDTO : dto.getHand()) {
            player.addCard(Card.fromDTO(cardDTO, context));
        }
        
        return player;
    }
}
```

### GamePhase.java (Actualizado)
```java
public enum GamePhase {
    WAITING,          // Esperando jugadores
    INITIAL_DECK,     // Fase 1: Mazo inicial
    VISIBLE_CARDS,    // Fase 2: Cartas visibles
    HIDDEN_CARDS,     // Fase 3: Cartas ocultas
    EXTRA_PHASE,      // Fase extra: Penalización
    FINISHED          // Juego terminado
}
```

### Nuevos Modelos (DTOs)

#### CardDTO.java
```java
public class CardDTO {
    private int value;      // 1-13
    private String suit;    // "HEARTS", "DIAMONDS", etc.
    
    // Getters, setters, constructores
}
```

#### GameStateDTO.java
```java
public class GameStateDTO {
    private String gameId;
    private List<PlayerDTO> players;
    private List<CardDTO> deck;
    private List<CardDTO> table;
    private int currentPlayerIndex;
    private String status;
    private String currentPhase;
    private String winnerId;
}
```

#### RoomResponse.java
```java
public class RoomResponse {
    private String gameId;
    private String playerId;
    private GameStateDTO gameState;
    private String message;
}
```

---

## 3. GameViewModel Completo

```java
public class GameViewModel extends ViewModel {
    
    // Modo de juego
    public enum GameMode {
        LOCAL,          // Offline (lógica local)
        ONLINE          // Online (servidor)
    }
    
    private GameMode currentMode = GameMode.LOCAL;
    private String gameId;
    private String playerId;
    private WebSocketManager wsManager;
    
    // LiveData existente
    private final MutableLiveData<List<Card>> _deck;
    private final MutableLiveData<List<Player>> _players;
    private final MutableLiveData<List<Card>> _tableCards;
    private final MutableLiveData<Integer> _currentPlayerIndex;
    private final MutableLiveData<GamePhase> _gamePhase;
    
    // NUEVO: LiveData para networking
    private final MutableLiveData<Boolean> isConnected;
    private final MutableLiveData<String> networkError;
    private final MutableLiveData<Boolean> isSyncing;
    
    // NUEVO: Crear sala online
    public void createOnlineRoom(String playerName, int botCount, 
                                  OnRoomCreatedListener listener) {
        currentMode = GameMode.ONLINE;
        
        CreateRoomRequest request = new CreateRoomRequest(
            playerName, false, 4, botCount
        );
        
        RetrofitInstance.getApiService().createRoom(request)
            .enqueue(new Callback<RoomResponse>() {
                @Override
                public void onResponse(Call<RoomResponse> call, 
                                        Response<RoomResponse> response) {
                    if (response.isSuccessful()) {
                        RoomResponse room = response.body();
                        gameId = room.getGameId();
                        playerId = room.getPlayerId();
                        
                        connectWebSocket();
                        updateFromServer(room.getGameState());
                        
                        listener.onSuccess(gameId, playerId);
                    }
                }
                
                @Override
                public void onFailure(Call<RoomResponse> call, Throwable t) {
                    listener.onError(t.getMessage());
                }
            });
    }
    
    // NUEVO: Unirse a sala
    public void joinOnlineRoom(String gameId, String playerName,
                                OnRoomJoinedListener listener) {
        currentMode = GameMode.ONLINE;
        
        JoinRoomRequest request = new JoinRoomRequest(gameId, playerName);
        
        RetrofitInstance.getApiService().joinRoom(request)
            .enqueue(new Callback<RoomResponse>() {
                @Override
                public void onResponse(Call<RoomResponse> call,
                                        Response<RoomResponse> response) {
                    if (response.isSuccessful()) {
                        RoomResponse room = response.body();
                        this.gameId = room.getGameId();
                        this.playerId = room.getPlayerId();
                        
                        connectWebSocket();
                        updateFromServer(room.getGameState());
                        
                        listener.onSuccess();
                    }
                }
                
                @Override
                public void onFailure(Call<RoomResponse> call, Throwable t) {
                    listener.onError(t.getMessage());
                }
            });
    }
    
    // NUEVO: Conectar WebSocket
    private void connectWebSocket() {
        wsManager = new WebSocketManager();
        wsManager.connect(gameId, new GameStateListener() {
            @Override
            public void onGameStateUpdate(GameStateUpdate update) {
                updateFromServer(update.getGameState());
                isConnected.postValue(true);
                isSyncing.postValue(false);
            }
            
            @Override
            public void onError(String error) {
                networkError.postValue(error);
                isConnected.postValue(false);
            }
        });
    }
    
    // NUEVO: Jugar carta (online o local)
    public void playCard(Card card) {
        if (currentMode == GameMode.LOCAL) {
            playCardLocal(card);  // Lógica local existente
        } else {
            playCardOnline(card);
        }
    }
    
    // NUEVO: Jugar carta online
    private void playCardOnline(Card card) {
        isSyncing.postValue(true);
        
        CardDTO cardDTO = card.toDTO();
        wsManager.playCard(gameId, playerId, cardDTO);
        
        // El servidor responderá vía WebSocket
    }
    
    // NUEVO: Actualizar desde servidor
    private void updateFromServer(GameStateDTO serverState) {
        Context context = getApplication().getApplicationContext();
        
        // Convertir jugadores
        List<Player> players = new ArrayList<>();
        for (PlayerDTO playerDTO : serverState.getPlayers()) {
            players.add(Player.fromDTO(playerDTO, context));
        }
        
        // Convertir mesa
        List<Card> table = new ArrayList<>();
        for (CardDTO cardDTO : serverState.getTable()) {
            table.add(Card.fromDTO(cardDTO, context));
        }
        
        // Actualizar LiveData
        _players.postValue(players);
        _tableCards.postValue(table);
        _currentPlayerIndex.postValue(serverState.getCurrentPlayerIndex());
        
        // Actualizar fase
        GamePhase phase = GamePhase.valueOf(serverState.getCurrentPhase());
        _gamePhase.postValue(phase);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (wsManager != null) {
            wsManager.disconnect();
        }
    }
}
```

---

## 4. Networking Completo

### RetrofitInstance.java
```java
public class RetrofitInstance {
    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static Retrofit retrofit;
    
    public static Retrofit getInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
            
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }
    
    public static Put0ApiService getApiService() {
        return getInstance().create(Put0ApiService.class);
    }
}
```

### Put0ApiService.java
```java
public interface Put0ApiService {
    @POST("/api/rooms/create")
    Call<RoomResponse> createRoom(@Body CreateRoomRequest request);
    
    @POST("/api/rooms/join")
    Call<RoomResponse> joinRoom(@Body JoinRoomRequest request);
    
    @POST("/api/rooms/{gameId}/start")
    Call<RoomResponse> startGame(@Path("gameId") String gameId);
    
    @GET("/api/rooms/{gameId}")
    Call<GameStateDTO> getGameState(@Path("gameId") String gameId);
}
```

### WebSocketManager.java
```java
public class WebSocketManager {
    private static final String WS_URL = "ws://10.0.2.2:8080/ws";
    private StompClient stompClient;
    private Disposable subscription;
    
    public void connect(String gameId, GameStateListener listener) {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);
        
        stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                if (lifecycleEvent.getType() == LifecycleEvent.Type.OPENED) {
                    subscribeToGame(gameId, listener);
                }
            });
        
        stompClient.connect();
    }
    
    private void subscribeToGame(String gameId, GameStateListener listener) {
        subscription = stompClient.topic("/topic/game/" + gameId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                GameStateUpdate update = new Gson()
                    .fromJson(topicMessage.getPayload(), GameStateUpdate.class);
                listener.onGameStateUpdate(update);
            });
    }
    
    public void playCard(String gameId, String playerId, CardDTO card) {
        PlayCardRequest request = new PlayCardRequest(gameId, playerId, card);
        String json = new Gson().toJson(request);
        
        stompClient.send("/app/game/play", json)
            .subscribeOn(Schedulers.io())
            .subscribe();
    }
    
    public void disconnect() {
        if (subscription != null) subscription.dispose();
        if (stompClient != null) stompClient.disconnect();
    }
}
```

---

## 5. UI/UX Completo

### MainActivity (Actualizado)
- Selector de modo: Local / Online
- Botón "Crear Sala"
- Botón "Unirse a Sala"
- Indicador de conexión

### GameActivity (Actualizado)
- Indicador de sincronización
- Indicador de turno (local o del servidor)
- Vista de cartas visibles
- Vista de cartas ocultas
- Indicador de fase actual
- Notificaciones de eventos del servidor

### Nuevo: LobbyActivity
- Lista de jugadores en sala
- Botón "Iniciar Juego" (solo host)
- Indicador de espera
- Chat (opcional)

---

## 6. Configuración

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

### network_security_config.xml
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

---

## 📊 Métricas Esperadas

| Componente | Completitud Esperada |
|------------|---------------------|
| Arquitectura | 100% |
| UI/UX | 100% |
| Modelos | 100% |
| ViewModels | 100% |
| Lógica Local | 100% |
| Networking | 100% |
| WebSocket | 100% |
| Reglas Juego | 100% |

**Completitud General Esperada: 100%**
