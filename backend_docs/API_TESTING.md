# PUT0 Backend - Postman API Testing Guide

Esta guía proporciona todos los endpoints REST del backend PUT0 con ejemplos detallados para configurar en Postman.

---

## Configuración Inicial

### Variables de Entorno en Postman

Crea un entorno en Postman con las siguientes variables:

| Variable | Valor Inicial | Descripción |
|----------|---------------|-------------|
| `baseUrl` | `http://localhost:8080` | URL base del servidor |
| `gameId` | *(vacío)* | Se llenará automáticamente |
| `playerId` | *(vacío)* | Se llenará automáticamente |

---

## 1. Health Check

**Propósito**: Verificar que el servidor está corriendo correctamente.

### Request
```
GET {{baseUrl}}/actuator/health
```

### Headers
```
(ninguno requerido)
```

### Response Esperado (200 OK)
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Server is UP", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.eql("UP");
});
```

---

## 2. Crear Sala (Solo con Bots)

**Propósito**: Crear una nueva sala de juego con jugador humano y bots AI.

### Request
```
POST {{baseUrl}}/api/rooms/create
```

### Headers
```
Content-Type: application/json
```

### Body (JSON)
```json
{
  "playerName": "Jugador1",
  "isPrivate": false,
  "maxPlayers": 4,
  "botCount": 1
}
```

### Parámetros del Body

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `playerName` | String | ✅ | Nombre del jugador humano |
| `isPrivate` | Boolean | ❌ | Si la sala es privada (default: false) |
| `maxPlayers` | Integer | ❌ | Máximo de jugadores (default: 4) |
| `botCount` | Integer | ❌ | Número de bots AI a agregar (default: 0) |

### Response Esperado (200 OK)
```json
{
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "playerId": "p1a2b3c4-d5e6-f789-0abc-def123456789",
  "gameState": {
    "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "players": [
      {
        "id": "p1a2b3c4-d5e6-f789-0abc-def123456789",
        "name": "Jugador1",
        "hand": [],
        "bot": false,
        "active": true,
        "cardCount": 0,
        "playableCards": []
      },
      {
        "id": "b1o2t3i4-d5e6-f789-0abc-def123456789",
        "name": "Bot 1",
        "hand": [],
        "bot": true,
        "active": true,
        "cardCount": 0,
        "playableCards": []
      }
    ],
    "deck": [],
    "table": [],
    "currentPlayerIndex": 0,
    "status": "WAITING",
    "winnerId": null,
    "topCard": null,
    "currentPlayer": {
      "id": "p1a2b3c4-d5e6-f789-0abc-def123456789",
      "name": "Jugador1",
      "hand": [],
      "bot": false,
      "active": true
    }
  },
  "message": "Room created successfully"
}
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has gameId and playerId", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.gameId).to.be.a('string');
    pm.expect(jsonData.playerId).to.be.a('string');
    
    // Guardar en variables de entorno
    pm.environment.set("gameId", jsonData.gameId);
    pm.environment.set("playerId", jsonData.playerId);
});

pm.test("Game status is WAITING", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.gameState.status).to.eql("WAITING");
});

pm.test("Correct number of players", function () {
    var jsonData = pm.response.json();
    var expectedPlayers = 2; // 1 humano + 1 bot
    pm.expect(jsonData.gameState.players).to.have.lengthOf(expectedPlayers);
});
```

---

## 3. Unirse a Sala Existente

**Propósito**: Unir un segundo jugador a una sala existente (multiplayer).

### Request
```
POST {{baseUrl}}/api/rooms/join
```

### Headers
```
Content-Type: application/json
```

### Body (JSON)
```json
{
  "gameId": "{{gameId}}",
  "playerName": "Jugador2"
}
```

### Parámetros del Body

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `gameId` | String | ✅ | ID de la sala a unirse |
| `playerName` | String | ✅ | Nombre del jugador |

### Response Esperado (200 OK)
```json
{
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "playerId": "p2x3y4z5-a6b7-c890-def1-234567890abc",
  "gameState": {
    "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "players": [
      {
        "id": "p1a2b3c4-d5e6-f789-0abc-def123456789",
        "name": "Jugador1",
        "bot": false
      },
      {
        "id": "b1o2t3i4-d5e6-f789-0abc-def123456789",
        "name": "Bot 1",
        "bot": true
      },
      {
        "id": "p2x3y4z5-a6b7-c890-def1-234567890abc",
        "name": "Jugador2",
        "bot": false
      }
    ],
    "status": "WAITING"
  },
  "message": "Joined room successfully"
}
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Player joined successfully", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.playerId).to.be.a('string');
    pm.expect(jsonData.message).to.include("successfully");
});

pm.test("Player count increased", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.gameState.players.length).to.be.at.least(2);
});
```

---

## 4. Iniciar Juego

**Propósito**: Iniciar el juego (reparte cartas y comienza el primer turno).

### Request
```
POST {{baseUrl}}/api/rooms/{{gameId}}/start
```

### Headers
```
Content-Type: application/json
```

### Body
```
(ninguno)
```

### Response Esperado (200 OK)
```json
{
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "playerId": null,
  "gameState": {
    "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "players": [
      {
        "id": "p1a2b3c4-d5e6-f789-0abc-def123456789",
        "name": "Jugador1",
        "hand": [
          {"value": 5, "suit": "HEARTS"},
          {"value": 10, "suit": "DIAMONDS"},
          {"value": 3, "suit": "CLUBS"}
        ],
        "bot": false,
        "cardCount": 26
      },
      {
        "id": "b1o2t3i4-d5e6-f789-0abc-def123456789",
        "name": "Bot 1",
        "hand": [
          {"value": 7, "suit": "SPADES"},
          {"value": 2, "suit": "HEARTS"}
        ],
        "bot": true,
        "cardCount": 26
      }
    ],
    "deck": [],
    "table": [],
    "currentPlayerIndex": 0,
    "status": "PLAYING",
    "winnerId": null
  },
  "message": "Game started successfully"
}
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Game status is PLAYING", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.gameState.status).to.eql("PLAYING");
});

pm.test("Cards were dealt", function () {
    var jsonData = pm.response.json();
    var player = jsonData.gameState.players[0];
    pm.expect(player.cardCount).to.be.above(0);
});

pm.test("Deck is empty after dealing", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.gameState.deck).to.have.lengthOf(0);
});
```

---

## 5. Obtener Estado del Juego

**Propósito**: Consultar el estado actual de una partida.

### Request
```
GET {{baseUrl}}/api/rooms/{{gameId}}
```

### Headers
```
(ninguno requerido)
```

### Response Esperado (200 OK)
```json
{
  "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "players": [...],
  "deck": [...],
  "table": [
    {"value": 5, "suit": "HEARTS"}
  ],
  "currentPlayerIndex": 1,
  "status": "PLAYING",
  "winnerId": null,
  "topCard": {"value": 5, "suit": "HEARTS"},
  "currentPlayer": {
    "id": "b1o2t3i4-d5e6-f789-0abc-def123456789",
    "name": "Bot 1",
    "bot": true
  }
}
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Game state is valid", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('gameId');
    pm.expect(jsonData).to.have.property('players');
    pm.expect(jsonData).to.have.property('status');
});
```

---

## 6. Listar Todas las Salas

**Propósito**: Obtener lista de todas las salas activas.

### Request
```
GET {{baseUrl}}/api/rooms
```

### Headers
```
(ninguno requerido)
```

### Response Esperado (200 OK)
```json
[
  {
    "gameId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "players": [
      {"id": "p1...", "name": "Jugador1", "bot": false},
      {"id": "b1...", "name": "Bot 1", "bot": true}
    ],
    "status": "PLAYING"
  },
  {
    "gameId": "x9y8z7w6-v5u4-t321-s098-r765q432p109",
    "players": [
      {"id": "p2...", "name": "Jugador2", "bot": false}
    ],
    "status": "WAITING"
  }
]
```

### Tests en Postman
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response is an array", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.be.an('array');
});
```

---

## Escenarios de Prueba Completos

### Escenario 1: Juego Solo (1 Humano vs 1 Bot)

**Flujo de prueba:**

1. **Crear sala con 1 bot**
   ```
   POST /api/rooms/create
   Body: {"playerName": "TestPlayer", "botCount": 1}
   ```

2. **Iniciar juego**
   ```
   POST /api/rooms/{{gameId}}/start
   ```

3. **Verificar estado**
   ```
   GET /api/rooms/{{gameId}}
   ```
   - Verificar que `status = "PLAYING"`
   - Verificar que hay 2 jugadores (1 humano + 1 bot)
   - Verificar que las cartas fueron repartidas

4. **Observar turnos del bot**
   - El bot debería jugar automáticamente
   - Consultar estado repetidamente para ver cambios

### Escenario 2: Juego Multijugador (2 Humanos)

**Flujo de prueba:**

1. **Jugador 1 crea sala**
   ```
   POST /api/rooms/create
   Body: {"playerName": "Player1", "botCount": 0}
   ```

2. **Jugador 2 se une**
   ```
   POST /api/rooms/join
   Body: {"gameId": "{{gameId}}", "playerName": "Player2"}
   ```

3. **Iniciar juego**
   ```
   POST /api/rooms/{{gameId}}/start
   ```

4. **Verificar estado**
   ```
   GET /api/rooms/{{gameId}}
   ```

### Escenario 3: Validación de Errores

**Pruebas de casos negativos:**

1. **Unirse a sala inexistente**
   ```
   POST /api/rooms/join
   Body: {"gameId": "invalid-id", "playerName": "Test"}
   ```
   - Esperado: 400 Bad Request

2. **Iniciar juego sin jugadores suficientes**
   ```
   POST /api/rooms/create (solo 1 jugador, sin bots)
   POST /api/rooms/{{gameId}}/start
   ```
   - Esperado: 400 Bad Request con mensaje de error

3. **Obtener sala inexistente**
   ```
   GET /api/rooms/invalid-game-id
   ```
   - Esperado: 404 Not Found

---

## Testing de WebSocket (Requiere Cliente WebSocket)

> [!NOTE]
> Los endpoints WebSocket no se pueden probar directamente en Postman. Usa herramientas como:
> - **Postman WebSocket** (nueva funcionalidad)
> - **wscat** (CLI)
> - **Cliente web personalizado**

### Conexión WebSocket

**Endpoint**: `ws://localhost:8080/ws`

**Protocolo**: STOMP over WebSocket

### Suscribirse a Actualizaciones del Juego

```javascript
// Conectar
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Suscribirse a un juego específico
stompClient.subscribe('/topic/game/' + gameId, function(message) {
    console.log('Update received:', JSON.parse(message.body));
});
```

### Jugar una Carta

```javascript
stompClient.send("/app/game/play", {}, JSON.stringify({
    gameId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    playerId: "p1a2b3c4-d5e6-f789-0abc-def123456789",
    card: {
        value: 10,
        suit: "HEARTS"
    }
}));
```

### Robar una Carta

```javascript
stompClient.send("/app/game/draw", {}, JSON.stringify({
    gameId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    playerId: "p1a2b3c4-d5e6-f789-0abc-def123456789"
}));
```

---

## Importar a Postman

### Opción 1: Crear Colección Manualmente

1. Crear nueva colección "PUT0 Backend API"
2. Agregar cada endpoint según esta documentación
3. Configurar variables de entorno
4. Agregar tests a cada request

### Opción 2: Usar JSON de Colección

Guarda el siguiente JSON como `PUT0_Postman_Collection.json` e impórtalo en Postman:

```json
{
  "info": {
    "name": "PUT0 Backend API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/actuator/health"
      }
    },
    {
      "name": "Create Room",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/rooms/create",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"playerName\": \"Player1\", \"botCount\": 1}"
        }
      }
    },
    {
      "name": "Join Room",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/rooms/join",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"gameId\": \"{{gameId}}\", \"playerName\": \"Player2\"}"
        }
      }
    },
    {
      "name": "Start Game",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/rooms/{{gameId}}/start",
        "header": [{"key": "Content-Type", "value": "application/json"}]
      }
    },
    {
      "name": "Get Game State",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/rooms/{{gameId}}"
      }
    },
    {
      "name": "List All Games",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/rooms"
      }
    }
  ]
}
```

---

## Resumen de Endpoints

| Método | Endpoint | Propósito |
|--------|----------|-----------|
| GET | `/actuator/health` | Verificar salud del servidor |
| POST | `/api/rooms/create` | Crear nueva sala |
| POST | `/api/rooms/join` | Unirse a sala existente |
| POST | `/api/rooms/{gameId}/start` | Iniciar juego |
| GET | `/api/rooms/{gameId}` | Obtener estado del juego |
| GET | `/api/rooms` | Listar todas las salas |
| WS | `/app/game/play` | Jugar carta (WebSocket) |
| WS | `/app/game/draw` | Robar carta (WebSocket) |

---

## Próximos Pasos

1. ✅ Importar colección a Postman
2. ✅ Configurar variables de entorno
3. ✅ Ejecutar escenarios de prueba
4. ✅ Validar respuestas y errores
5. ⏭️ Proceder con integración Android
