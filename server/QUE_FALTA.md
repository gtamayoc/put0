# PUT0 Server - Tareas Pendientes

## 🎯 Resumen Ejecutivo

**Total de Tareas**: 67  
**Prioridad Alta**: 28  
**Prioridad Media**: 24  
**Prioridad Baja**: 15  

---

## 📋 Fase 1: Corrección de Reglas Básicas (Prioridad ALTA)

### 1.1 Modelos de Datos

- [ ] **Card.java** - Agregar método `getNumericValue()`
  - As (value=1) debe retornar 14
  - 2-K retornan su valor normal
  - Estimado: 15 min

- [ ] **Card.java** - Actualizar `canPlayOn()` para cartas especiales
  - A, 2, 10 pueden jugarse siempre
  - Mantener regla normal para otras cartas
  - Estimado: 20 min

- [ ] **Player.java** - Agregar `List<Card> hiddenCards`
  - Constructor inicializa lista vacía
  - Métodos: `addHiddenCard()`, `getRandomHiddenCard()`
  - Estimado: 10 min

- [ ] **Player.java** - Agregar `List<Card> visibleCards`
  - Constructor inicializa lista vacía
  - Métodos: `addVisibleCard()`, `removeVisibleCard()`
  - Estimado: 10 min

- [ ] **Player.java** - Agregar `String currentGameId`
  - Para tracking de partida activa
  - Métodos: `setCurrentGame()`, `getCurrentGame()`
  - Estimado: 5 min

- [ ] **GameState.java** - Agregar `GamePhase currentPhase`
  - Inicializar en INITIAL_DECK
  - Método: `advancePhase()`
  - Estimado: 10 min

- [ ] **GameState.java** - Agregar `List<Card> discardPile`
  - Para cartas descartadas
  - Método: `addToDiscardPile()`
  - Estimado: 5 min

- [ ] **Crear GamePhase.java** - Enum con 4 fases
  - INITIAL_DECK, VISIBLE_CARDS, HIDDEN_CARDS, EXTRA_PHASE
  - Estimado: 5 min

### 1.2 Lógica de Juego

- [ ] **GameEngine.createDeck()** - Crear 2 mazos (104 cartas)
  - Duplicar loop de creación
  - Verificar con test
  - Estimado: 10 min

- [ ] **GameEngine.dealCards()** - Repartir 3+3+3 por jugador
  - 3 ocultas, 3 visibles, 3 mano
  - Colocar carta inicial en mesa
  - Estimado: 30 min

- [ ] **GameEngine.playCard()** - Robo automático después de jugar
  - Si deck no vacío y mano < 3
  - Agregar carta a mano
  - Estimado: 15 min

- [ ] **GameEngine.playCard()** - Robo extra con 10
  - Si carta es 10, robar 1 extra
  - Si quedan 2 cartas, robar otra
  - Estimado: 20 min

- [ ] **GameEngine** - Implementar `playVisibleCard()`
  - Validar fase VISIBLE_CARDS
  - Jugar carta del índice especificado
  - Avanzar fase si no quedan visibles
  - Estimado: 30 min

- [ ] **GameEngine** - Implementar `playHiddenCard()`
  - Validar fase HIDDEN_CARDS
  - Robar carta oculta al azar
  - Si < mesa, recoger todas las cartas
  - Estimado: 40 min

- [ ] **GameEngine** - Implementar `pickUpTableCards()`
  - Agregar cartas de mesa a mano del jugador
  - Limpiar mesa
  - Cambiar a fase EXTRA_PHASE
  - Estimado: 20 min

- [ ] **GameEngine** - Implementar `swapInitialCards()`
  - Intercambiar carta visible con carta de mano
  - Solo permitido antes de iniciar juego
  - Estimado: 25 min

---

## 📋 Fase 2: Gestión de Sesiones (Prioridad ALTA)

### 2.1 Nuevo Servicio

- [ ] **Crear PlayerSession.java** - Modelo de sesión
  - playerId, currentGameId, joinedAt, lastActivity
  - Estimado: 10 min

- [ ] **Crear SessionManager.java** - Servicio de gestión
  - Map<String, PlayerSession> activeSessions
  - Métodos: canJoinGame(), joinGame(), leaveGame()
  - Estimado: 45 min

- [ ] **SessionManager** - Implementar validación única
  - Verificar que jugador no esté en otra partida
  - Lanzar excepción si ya está activo
  - Estimado: 15 min

- [ ] **SessionManager** - Implementar limpieza automática
  - @Scheduled cada minuto
  - Remover sesiones inactivas > 5 min
  - Estimado: 20 min

### 2.2 Integración

- [ ] **RoomController.createRoom()** - Validar sesión
  - Llamar sessionManager.canJoinGame()
  - Registrar con sessionManager.joinGame()
  - Estimado: 10 min

- [ ] **RoomController.joinRoom()** - Validar sesión
  - Llamar sessionManager.canJoinGame()
  - Registrar con sessionManager.joinGame()
  - Estimado: 10 min

- [ ] **RoomController** - Nuevo endpoint `/leave`
  - Validar que juego no esté PLAYING
  - Llamar sessionManager.leaveGame()
  - Estimado: 20 min

---

## 📋 Fase 3: AI Bot Mejorado (Prioridad MEDIA)

- [ ] **AIBotService** - Estrategia para cartas especiales
  - Priorizar A y 2 cuando sea ventajoso
  - Estimado: 30 min

- [ ] **AIBotService** - Lógica para cartas visibles
  - Elegir mejor carta visible para jugar
  - Estimado: 25 min

- [ ] **AIBotService** - Lógica para cartas ocultas
  - Decidir cuándo jugar ocultas
  - Estimado: 20 min

---

## 📋 Fase 4: API REST Completa (Prioridad MEDIA)

- [ ] **RoomController** - Endpoint `/swap`
  - POST /api/rooms/{gameId}/swap
  - Intercambiar cartas iniciales
  - Estimado: 25 min

- [ ] **RoomController** - Endpoint `/surrender`
  - POST /api/rooms/{gameId}/surrender
  - Marcar jugador como perdedor
  - Estimado: 20 min

- [ ] **RoomController** - Validación de errores mejorada
  - Mensajes de error más descriptivos
  - Códigos HTTP apropiados
  - Estimado: 30 min

---

## 📋 Fase 5: WebSocket Completo (Prioridad MEDIA)

- [ ] **GameWebSocketController** - Endpoint `/game/playVisible`
  - @MessageMapping("/game/playVisible")
  - PlayVisibleCardRequest DTO
  - Estimado: 20 min

- [ ] **GameWebSocketController** - Endpoint `/game/playHidden`
  - @MessageMapping("/game/playHidden")
  - PlayHiddenCardRequest DTO
  - Estimado: 20 min

- [ ] **GameWebSocketController** - Endpoint `/game/swap`
  - @MessageMapping("/game/swap")
  - SwapCardsRequest DTO
  - Estimado: 20 min

- [ ] **WebSocketConfig** - Manejo de desconexión
  - Detectar cuando jugador se desconecta
  - Actualizar lastActivity
  - Estimado: 30 min

---

## 📋 Fase 6: DTOs Adicionales (Prioridad BAJA)

- [ ] **Crear PlayVisibleCardRequest.java**
  - gameId, playerId, cardIndex
  - Estimado: 5 min

- [ ] **Crear PlayHiddenCardRequest.java**
  - gameId, playerId
  - Estimado: 5 min

- [ ] **Crear SwapCardsRequest.java**
  - gameId, playerId, visibleIndex, handIndex
  - Estimado: 5 min

- [ ] **Crear PlayerSessionDTO.java**
  - Para respuestas de sesión
  - Estimado: 5 min

---

## 📋 Fase 7: Tests Completos (Prioridad ALTA)

### 7.1 Tests de Modelos

- [ ] **CardTest** - `testAceIsHighestCard()`
  - Verificar A > K
  - Estimado: 10 min

- [ ] **CardTest** - `testTwoIsSpecialCard()`
  - Verificar 2 puede jugarse siempre
  - Estimado: 10 min

- [ ] **CardTest** - `testSpecialCardsCanPlayAnytime()`
  - Verificar A, 2, 10 especiales
  - Estimado: 15 min

- [ ] **PlayerTest** - `testHiddenAndVisibleCards()`
  - Verificar gestión de cartas ocultas/visibles
  - Estimado: 15 min

- [ ] **GameStateTest** - `testPhaseProgression()`
  - Verificar avance de fases
  - Estimado: 20 min

### 7.2 Tests de GameEngine

- [ ] **GameEngineTest** - `testCreateTwoDecks()`
  - Verificar 104 cartas
  - Estimado: 10 min

- [ ] **GameEngineTest** - `testDealNineCardsPerPlayer()`
  - Verificar 3+3+3
  - Estimado: 15 min

- [ ] **GameEngineTest** - `testInitialCardOnTable()`
  - Verificar carta inicial
  - Estimado: 10 min

- [ ] **GameEngineTest** - `testAutoDrawAfterPlay()`
  - Verificar robo automático
  - Estimado: 15 min

- [ ] **GameEngineTest** - `testExtraDrawWith10()`
  - Verificar robo extra con 10
  - Estimado: 15 min

- [ ] **GameEngineTest** - `testPlayVisibleCard()`
  - Verificar Fase 2
  - Estimado: 20 min

- [ ] **GameEngineTest** - `testPlayHiddenCard()`
  - Verificar Fase 3
  - Estimado: 20 min

- [ ] **GameEngineTest** - `testPickUpCards()`
  - Verificar recoger cartas de mesa
  - Estimado: 20 min

### 7.3 Tests de Sesiones

- [ ] **SessionManagerTest** - `testPlayerCannotJoinTwoGames()`
  - Verificar validación única
  - Estimado: 15 min

- [ ] **SessionManagerTest** - `testPlayerCanLeaveFinishedGame()`
  - Verificar salida permitida
  - Estimado: 10 min

- [ ] **SessionManagerTest** - `testPlayerCannotLeavePlayingGame()`
  - Verificar salida bloqueada
  - Estimado: 10 min

- [ ] **SessionManagerTest** - `testInactiveSessionCleanup()`
  - Verificar limpieza automática
  - Estimado: 15 min

---

## 📋 Fase 8: Documentación (Prioridad BAJA)

- [ ] **Actualizar API_TESTING.md** - Nuevos endpoints
  - Documentar /swap, /surrender, /leave
  - Estimado: 30 min

- [ ] **Actualizar API_TESTING.md** - WebSocket adicionales
  - Documentar /game/playVisible, /game/playHidden
  - Estimado: 20 min

- [ ] **Crear GAME_RULES.md** - Documentación de reglas
  - Todas las reglas implementadas
  - Ejemplos de cada fase
  - Estimado: 60 min

---

## 📊 Estimación de Tiempo Total

| Fase | Tareas | Tiempo Estimado |
|------|--------|-----------------|
| Fase 1: Reglas Básicas | 17 | ~5 horas |
| Fase 2: Sesiones | 7 | ~2.5 horas |
| Fase 3: AI Bot | 3 | ~1.5 horas |
| Fase 4: API REST | 3 | ~1.5 horas |
| Fase 5: WebSocket | 4 | ~1.5 horas |
| Fase 6: DTOs | 4 | ~20 min |
| Fase 7: Tests | 16 | ~4 horas |
| Fase 8: Documentación | 3 | ~2 horas |

**TOTAL: ~18 horas de desarrollo**

---

## 🚀 Orden de Implementación Recomendado

1. ✅ **Fase 1** (Reglas Básicas) - CRÍTICO
2. ✅ **Fase 7** (Tests) - Validar cambios
3. ✅ **Fase 2** (Sesiones) - CRÍTICO
4. ✅ **Fase 5** (WebSocket) - Para integración Android
5. ✅ **Fase 4** (API REST) - Completar endpoints
6. ✅ **Fase 3** (AI Bot) - Mejorar experiencia
7. ✅ **Fase 6** (DTOs) - Soporte
8. ✅ **Fase 8** (Documentación) - Final
