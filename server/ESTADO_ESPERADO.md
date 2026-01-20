# PUT0 Server - Estado Esperado

## 🎯 Objetivo Final

Servidor Spring Boot completamente funcional que implemente todas las reglas del juego PUT0 según README.md, con soporte para:
- ✅ Modo Solo (1 humano + bots)
- ✅ Modo Multijugador (2-6 jugadores humanos)
- ✅ Todas las fases del juego
- ✅ Gestión de sesiones (1 jugador = 1 partida)
- ✅ Sincronización en tiempo real vía WebSocket

---

## 1. Modelos de Datos Completos

### Card.java
```java
public class Card {
    private int value;           // 1-13 (A=1, pero numéricamente=14)
    private Suit suit;
    
    // Métodos esperados
    public int getNumericValue();  // A=14, 2-K=2-13
    public boolean canPlayOn(Card tableCard);  // A, 2, 10 especiales
    public boolean clearsTable();  // Solo 10
    public boolean isSpecial();    // A, 2, 10
}
```

### Player.java
```java
public class Player {
    private String id;
    private String name;
    private List<Card> hand;           // Máximo 3 cartas
    private List<Card> hiddenCards;    // 3 cartas ocultas
    private List<Card> visibleCards;   // 3 cartas visibles
    private boolean isBot;
    private boolean isActive;
    private String currentGameId;      // NUEVO: Tracking de partida activa
    
    // Métodos esperados
    public boolean canPlayCard(Card card, Card topCard);
    public Card getRandomHiddenCard();
    public boolean hasCardsInPhase(GamePhase phase);
    public void swapVisibleCard(int visibleIndex, int handIndex);
}
```

### GameState.java
```java
public class GameState {
    private String gameId;
    private List<Player> players;
    private List<Card> deck;           // 104 cartas (2 mazos)
    private List<Card> table;
    private List<Card> discardPile;
    private int currentPlayerIndex;
    private GameStatus status;
    private GamePhase currentPhase;    // NUEVO
    private String winnerId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    
    // Métodos esperados
    public Player getCurrentPlayer();
    public Card getTopCard();
    public void nextTurn();
    public void clearTable();
    public boolean shouldClearTable();
    public void advancePhase();
    public boolean isPlayerInGame(String playerId);
}
```

### GamePhase.java (NUEVO)
```java
public enum GamePhase {
    INITIAL_DECK,    // Fase 1: Jugando del mazo inicial
    VISIBLE_CARDS,   // Fase 2: Jugando cartas visibles
    HIDDEN_CARDS,    // Fase 3: Jugando cartas ocultas
    EXTRA_PHASE      // Fase extra: Recogió en última jugada
}
```

### PlayerSession.java (NUEVO)
```java
public class PlayerSession {
    private String playerId;
    private String currentGameId;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActivity;
    private boolean isActive;
}
```

---

## 2. Lógica de Juego Completa (GameEngine)

### Creación y Configuración
```java
// Crear 2 mazos (104 cartas)
private List<Card> createDeck() {
    List<Card> deck = new ArrayList<>();
    for (int i = 0; i < 2; i++) {  // 2 mazos
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 13; value++) {
                deck.add(new Card(value, suit));
            }
        }
    }
    return deck;  // 104 cartas total
}

// Repartir 3+3+3 por jugador
private void dealCards(GameState game) {
    for (Player player : game.getPlayers()) {
        // 3 cartas ocultas
        for (int i = 0; i < 3; i++) {
            player.addHiddenCard(deck.remove(0));
        }
        // 3 cartas visibles
        for (int i = 0; i < 3; i++) {
            player.addVisibleCard(deck.remove(0));
        }
        // 3 cartas en mano
        for (int i = 0; i < 3; i++) {
            player.addCard(deck.remove(0));
        }
    }
    
    // Carta inicial aleatoria en mesa
    game.getTable().add(deck.remove(0));
}
```

### Jugar Carta con Robo Automático
```java
public void playCard(String gameId, String playerId, Card card) {
    // Validaciones...
    
    // Remover de mano
    currentPlayer.removeCard(card);
    
    // Agregar a mesa
    game.getTable().add(card);
    
    // ROBO AUTOMÁTICO (si hay mazo y mano < 3)
    if (!game.getDeck().isEmpty() && currentPlayer.getHand().size() < 3) {
        currentPlayer.addCard(game.getDeck().remove(0));
    }
    
    // Verificar limpieza
    if (card.getValue() == 10 || game.shouldClearTable()) {
        game.clearTable();
        
        // ROBO EXTRA CON 10
        if (card.getValue() == 10 && !game.getDeck().isEmpty()) {
            currentPlayer.addCard(game.getDeck().remove(0));
            
            // Si ahora tienes 2 cartas, roba otra
            if (currentPlayer.getHand().size() == 2 && !game.getDeck().isEmpty()) {
                currentPlayer.addCard(game.getDeck().remove(0));
            }
        }
        
        return;  // Mismo jugador juega de nuevo
    }
    
    // Verificar ganador
    if (currentPlayer.hasWon()) {
        game.setStatus(GameStatus.FINISHED);
        game.setWinnerId(playerId);
        return;
    }
    
    // Siguiente turno
    game.nextTurn();
}
```

### Fases del Juego
```java
// Jugar carta visible (Fase 2)
public void playVisibleCard(String gameId, String playerId, int cardIndex) {
    Player player = getCurrentPlayer();
    Card card = player.getVisibleCards().get(cardIndex);
    
    // Validar y jugar
    playCard(gameId, playerId, card);
    player.getVisibleCards().remove(cardIndex);
    
    // Si no quedan visibles, avanzar a Fase 3
    if (player.getVisibleCards().isEmpty()) {
        game.setCurrentPhase(GamePhase.HIDDEN_CARDS);
    }
}

// Jugar carta oculta (Fase 3)
public void playHiddenCard(String gameId, String playerId) {
    Player player = getCurrentPlayer();
    Card hiddenCard = player.getRandomHiddenCard();
    Card topCard = game.getTopCard();
    
    // Si carta oculta < mesa → RECOGER
    if (hiddenCard.getNumericValue() < topCard.getNumericValue()) {
        player.getHand().addAll(game.getTable());
        game.clearTable();
        game.setCurrentPhase(GamePhase.EXTRA_PHASE);
        return;
    }
    
    // Jugar normalmente
    playCard(gameId, playerId, hiddenCard);
}
```

---

## 3. Gestión de Sesiones

### SessionManager.java (NUEVO)
```java
@Service
public class SessionManager {
    private Map<String, PlayerSession> activeSessions;
    
    // Validar que jugador no esté en otra partida
    public boolean canJoinGame(String playerId) {
        PlayerSession session = activeSessions.get(playerId);
        return session == null || !session.isActive();
    }
    
    // Registrar jugador en partida
    public void joinGame(String playerId, String gameId) {
        if (!canJoinGame(playerId)) {
            throw new IllegalStateException("Player already in active game");
        }
        activeSessions.put(playerId, new PlayerSession(playerId, gameId));
    }
    
    // Liberar jugador (solo si juego terminó)
    public void leaveGame(String playerId, String gameId) {
        GameState game = gameEngine.getGame(gameId);
        if (game.getStatus() == GameStatus.PLAYING) {
            throw new IllegalStateException("Cannot leave game in progress");
        }
        activeSessions.remove(playerId);
    }
    
    // Limpieza automática (timeout 5 min)
    @Scheduled(fixedRate = 60000)  // Cada minuto
    public void cleanupInactiveSessions() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(5);
        activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(timeout)
        );
    }
}
```

---

## 4. API REST Completa

### Endpoints Adicionales
```java
// Salir de sala (solo si no está PLAYING)
@PostMapping("/api/rooms/{gameId}/leave")
public ResponseEntity<?> leaveRoom(@PathVariable String gameId, 
                                    @RequestParam String playerId);

// Intercambiar cartas iniciales
@PostMapping("/api/rooms/{gameId}/swap")
public ResponseEntity<?> swapCards(@PathVariable String gameId,
                                    @RequestBody SwapCardsRequest request);

// Rendirse (marca como perdedor)
@PostMapping("/api/rooms/{gameId}/surrender")
public ResponseEntity<?> surrender(@PathVariable String gameId,
                                    @RequestParam String playerId);
```

---

## 5. WebSocket Completo

### Endpoints Adicionales
```java
// Jugar carta visible
@MessageMapping("/game/playVisible")
public void playVisibleCard(PlayVisibleCardRequest request);

// Jugar carta oculta
@MessageMapping("/game/playHidden")
public void playHiddenCard(PlayHiddenCardRequest request);

// Intercambiar cartas
@MessageMapping("/game/swap")
public void swapCards(SwapCardsRequest request);
```

---

## 6. Tests Completos

### Nuevos Tests Requeridos
```java
// CardTest
- testAceIsHighestCard()
- testTwoIsSpecialCard()
- testSpecialCardsCanPlayAnytime()

// GameEngineTest
- testCreateTwoDecks()  // 104 cartas
- testDealNineCardsPerPlayer()  // 3+3+3
- testInitialCardOnTable()
- testAutoDrawAfterPlay()
- testExtraDrawWith10()
- testPhase2VisibleCards()
- testPhase3HiddenCards()
- testPickUpCardsFromTable()

// SessionManagerTest
- testPlayerCannotJoinTwoGames()
- testPlayerCanLeaveFinishedGame()
- testPlayerCannotLeavePlayingGame()
- testInactiveSessionCleanup()
```

---

## 7. Validaciones de Reglas

### Todas las Reglas Implementadas
- ✅ A (As) es la carta más alta (valor numérico 14)
- ✅ 2 puede jugarse en cualquier momento
- ✅ 10 limpia mesa + roba extra + juega nueva carta
- ✅ 4 cartas iguales limpian mesa automáticamente
- ✅ Robo automático después de jugar (si hay mazo)
- ✅ Carta inicial aleatoria en mesa
- ✅ Reparto 3+3+3 por jugador (104 cartas total)
- ✅ Fase 1: Mazo inicial
- ✅ Fase 2: Cartas visibles
- ✅ Fase 3: Cartas ocultas (robar al azar)
- ✅ Fase Extra: Si recoges en última jugada
- ✅ Recoger cartas si oculta < mesa
- ✅ Opción de reemplazo en reparto inicial

---

## 8. Gestión de Partidas

### Reglas de Sesión
- ✅ 1 jugador solo puede estar en 1 partida activa
- ✅ No puede salir de partida en estado PLAYING
- ✅ Puede salir de partida en estado WAITING o FINISHED
- ✅ Timeout de inactividad: 5 minutos
- ✅ Limpieza automática de partidas abandonadas
- ✅ Reconexión permitida (mismo playerId)

---

## 📊 Métricas Esperadas

| Componente | Completitud Esperada |
|------------|---------------------|
| Arquitectura | 100% |
| Modelos | 100% |
| Lógica Core | 100% |
| API REST | 100% |
| WebSocket | 100% |
| AI Bot | 100% |
| Tests | 100% |
| Reglas Juego | 100% |
| Sesiones | 100% |

**Completitud General Esperada: 100%**
