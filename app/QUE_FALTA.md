# PUT0 Android - Tareas Pendientes

## 🎯 Resumen Ejecutivo

**Total de Tareas**: 52  
**Prioridad Alta**: 22  
**Prioridad Media**: 18  
**Prioridad Baja**: 12  

---

## 📋 Fase 1: Configuración y Dependencias (Prioridad ALTA)

### 1.1 Dependencias

- [ ] **build.gradle.kts** - Agregar Retrofit
  - `com.squareup.retrofit2:retrofit:2.9.0`
  - `com.squareup.retrofit2:converter-gson:2.9.0`
  - Estimado: 5 min

- [ ] **build.gradle.kts** - Agregar OkHttp
  - `com.squareup.okhttp3:okhttp:4.11.0`
  - `com.squareup.okhttp3:logging-interceptor:4.11.0`
  - Estimado: 5 min

- [ ] **build.gradle.kts** - Agregar STOMP/WebSocket
  - `com.github.NaikSoftware:StompProtocolAndroid:1.6.6`
  - `io.reactivex.rxjava2:rxjava:2.2.21`
  - `io.reactivex.rxjava2:rxandroid:2.1.1`
  - Estimado: 5 min

- [ ] **build.gradle.kts** - Agregar Coroutines
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.6.2`
  - Estimado: 5 min

### 1.2 Configuración

- [ ] **AndroidManifest.xml** - Agregar permisos
  - INTERNET, ACCESS_NETWORK_STATE
  - usesCleartextTraffic=true
  - Estimado: 5 min

- [ ] **Crear network_security_config.xml**
  - Permitir cleartext para 10.0.2.2 y localhost
  - Estimado: 10 min

---

## 📋 Fase 2: Modelos y DTOs (Prioridad ALTA)

### 2.1 DTOs Nuevos

- [ ] **Crear CardDTO.java**
  - int value, String suit
  - Estimado: 10 min

- [ ] **Crear PlayerDTO.java**
  - String id, name, List<CardDTO> hand, etc.
  - Estimado: 15 min

- [ ] **Crear GameStateDTO.java**
  - gameId, players, deck, table, status, etc.
  - Estimado: 20 min

- [ ] **Crear RoomResponse.java**
  - gameId, playerId, gameState, message
  - Estimado: 10 min

- [ ] **Crear CreateRoomRequest.java**
  - playerName, isPrivate, maxPlayers, botCount
  - Estimado: 10 min

- [ ] **Crear JoinRoomRequest.java**
  - gameId, playerName
  - Estimado: 5 min

- [ ] **Crear PlayCardRequest.java**
  - gameId, playerId, card
  - Estimado: 10 min

- [ ] **Crear GameStateUpdate.java**
  - gameState, message, type
  - Estimado: 10 min

### 2.2 Conversores

- [ ] **Card.java** - Método `toDTO()`
  - Convertir String → int para value
  - Convertir String → Enum para suit
  - Estimado: 20 min

- [ ] **Card.java** - Método estático `fromDTO()`
  - Convertir int → String para value
  - Convertir Enum → String para suit
  - Obtener resourceId
  - Estimado: 30 min

- [ ] **Player.java** - Método estático `fromDTO()`
  - Convertir PlayerDTO → Player
  - Convertir todas las cartas
  - Estimado: 25 min

### 2.3 Actualizar Modelos Existentes

- [ ] **Player.java** - Agregar `String playerId`
  - Campo para ID del servidor
  - Getter y setter
  - Estimado: 5 min

- [ ] **Player.java** - Agregar `boolean isBot`
  - Campo para identificar bots
  - Getter y setter
  - Estimado: 5 min

- [ ] **GamePhase.java** - Agregar fases reales
  - INITIAL_DECK, VISIBLE_CARDS, HIDDEN_CARDS, EXTRA_PHASE
  - Estimado: 5 min

---

## 📋 Fase 3: Networking (Prioridad ALTA)

### 3.1 Retrofit

- [ ] **Crear RetrofitInstance.java**
  - Singleton de Retrofit
  - OkHttpClient con logging
  - BASE_URL configurable
  - Estimado: 30 min

- [ ] **Crear Put0ApiService.java**
  - Interface con endpoints REST
  - createRoom, joinRoom, startGame, getGameState
  - Estimado: 25 min

### 3.2 WebSocket

- [ ] **Crear WebSocketManager.java**
  - StompClient configuration
  - connect(), disconnect()
  - subscribe(), send()
  - Estimado: 60 min

- [ ] **Crear GameStateListener.java**
  - Interface para callbacks
  - onGameStateUpdate(), onError()
  - Estimado: 10 min

---

## 📋 Fase 4: GameViewModel (Prioridad ALTA)

### 4.1 Modo de Juego

- [ ] **GameViewModel** - Agregar enum `GameMode`
  - LOCAL, ONLINE
  - Estimado: 5 min

- [ ] **GameViewModel** - Agregar campos de red
  - gameId, playerId, wsManager
  - isConnected, networkError, isSyncing
  - Estimado: 10 min

### 4.2 Métodos Online

- [ ] **GameViewModel** - Implementar `createOnlineRoom()`
  - Llamar API REST
  - Conectar WebSocket
  - Actualizar estado
  - Estimado: 45 min

- [ ] **GameViewModel** - Implementar `joinOnlineRoom()`
  - Llamar API REST
  - Conectar WebSocket
  - Actualizar estado
  - Estimado: 40 min

- [ ] **GameViewModel** - Implementar `connectWebSocket()`
  - Crear WebSocketManager
  - Suscribirse a sala
  - Manejar callbacks
  - Estimado: 30 min

- [ ] **GameViewModel** - Implementar `playCardOnline()`
  - Convertir Card → CardDTO
  - Enviar vía WebSocket
  - Estimado: 20 min

- [ ] **GameViewModel** - Implementar `updateFromServer()`
  - Convertir DTOs → Modelos
  - Actualizar LiveData
  - Estimado: 40 min

- [ ] **GameViewModel** - Actualizar `playCard()`
  - Detectar modo (LOCAL vs ONLINE)
  - Delegar a método apropiado
  - Estimado: 15 min

### 4.3 Lifecycle

- [ ] **GameViewModel** - Implementar `onCleared()`
  - Desconectar WebSocket
  - Limpiar recursos
  - Estimado: 10 min

---

## 📋 Fase 5: UI/UX (Prioridad MEDIA)

### 5.1 MainActivity

- [ ] **MainActivity** - Agregar selector de modo
  - Botones: "Jugar Local" / "Jugar Online"
  - Estimado: 30 min

- [ ] **MainActivity** - Agregar "Crear Sala"
  - Dialog para nombre y número de bots
  - Llamar createOnlineRoom()
  - Estimado: 40 min

- [ ] **MainActivity** - Agregar "Unirse a Sala"
  - Dialog para gameId y nombre
  - Llamar joinOnlineRoom()
  - Estimado: 35 min

### 5.2 GameActivity

- [ ] **GameActivity** - Agregar indicador de conexión
  - Observar isConnected LiveData
  - Mostrar icono/color
  - Estimado: 20 min

- [ ] **GameActivity** - Agregar indicador de sincronización
  - Observar isSyncing LiveData
  - Mostrar ProgressBar
  - Estimado: 15 min

- [ ] **GameActivity** - Agregar indicador de fase
  - Mostrar fase actual del juego
  - Estimado: 25 min

- [ ] **GameActivity** - Agregar vista de cartas visibles
  - RecyclerView horizontal
  - Estimado: 40 min

- [ ] **GameActivity** - Agregar vista de cartas ocultas
  - Mostrar reverso de cartas
  - Estimado: 30 min

### 5.3 Nuevo: LobbyActivity

- [ ] **Crear LobbyActivity.java**
  - Pantalla de espera
  - Lista de jugadores
  - Botón "Iniciar Juego"
  - Estimado: 90 min

---

## 📋 Fase 6: Utils (Prioridad MEDIA)

- [ ] **Crear NetworkUtils.java**
  - Verificar conectividad
  - isNetworkAvailable()
  - Estimado: 20 min

- [ ] **Crear ErrorHandler.java**
  - Manejo centralizado de errores
  - Mostrar Toasts/Snackbars
  - Estimado: 30 min

---

## 📋 Fase 7: Adaptadores (Prioridad BAJA)

- [ ] **CardAdapter** - Actualización en tiempo real
  - Observar cambios desde WebSocket
  - Animaciones suaves
  - Estimado: 30 min

- [ ] **PlayerAdapter** - Indicador de turno del servidor
  - Mostrar turno actual
  - Highlight del jugador activo
  - Estimado: 25 min

---

## 📋 Fase 8: Testing (Prioridad BAJA)

- [ ] **Test** - Conversión Card ↔ CardDTO
  - Verificar mapeo correcto
  - Estimado: 20 min

- [ ] **Test** - Retrofit API calls
  - Mock server
  - Verificar requests/responses
  - Estimado: 40 min

- [ ] **Test** - WebSocket connection
  - Mock STOMP
  - Verificar suscripción
  - Estimado: 45 min

- [ ] **Test** - GameViewModel online
  - Mock networking
  - Verificar flujo completo
  - Estimado: 60 min

---

## 📊 Estimación de Tiempo Total

| Fase | Tareas | Tiempo Estimado |
|------|--------|-----------------|
| Fase 1: Configuración | 6 | ~40 min |
| Fase 2: Modelos/DTOs | 14 | ~3.5 horas |
| Fase 3: Networking | 4 | ~2 horas |
| Fase 4: GameViewModel | 10 | ~4 horas |
| Fase 5: UI/UX | 11 | ~6 horas |
| Fase 6: Utils | 2 | ~50 min |
| Fase 7: Adaptadores | 2 | ~1 hora |
| Fase 8: Testing | 4 | ~3 horas |

**TOTAL: ~21 horas de desarrollo**

---

## 🚀 Orden de Implementación Recomendado

1. ✅ **Fase 1** (Configuración) - Base necesaria
2. ✅ **Fase 2** (Modelos/DTOs) - Comunicación con servidor
3. ✅ **Fase 3** (Networking) - Conectividad
4. ✅ **Fase 4** (GameViewModel) - Lógica de integración
5. ✅ **Fase 5** (UI/UX) - Interfaz de usuario
6. ✅ **Fase 6** (Utils) - Soporte
7. ✅ **Fase 7** (Adaptadores) - Mejoras visuales
8. ✅ **Fase 8** (Testing) - Validación final
