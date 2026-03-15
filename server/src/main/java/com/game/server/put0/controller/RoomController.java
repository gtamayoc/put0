package com.game.server.put0.controller;

import com.game.server.put0.dto.*;
import com.game.core.model.GameState;
import com.game.server.put0.exception.GameNotFoundException;
import com.game.server.put0.service.AIBotService;
import com.game.server.put0.service.GameEngine;
import com.game.server.put0.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;

/**
 * REST API controller for room/lobby management.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow all origins for development
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);
    
    private final RoomService roomService;
    private final GameEngine gameEngine;
    private final AIBotService aiBotService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Creates a new game room.
     * POST /api/rooms/create
     */
    @PostMapping("/create")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        if (request.mode() == null) {
            return ResponseEntity.badRequest()
                    .body(new RoomResponse(null, null, null, "Match Mode is required", null));
        }
        
        RoomService.RoomCreationResult result = roomService.createRoom(
                request.playerName(),
                request.botCount(),
                request.mode()
        );
        
        GameState game = gameEngine.getGame(result.gameId())
                .orElseThrow(() -> new IllegalStateException("Game not found after creation"));
        
        RoomResponse response = new RoomResponse(
                result.gameId(),
                result.playerId(),
                game,
                "Room created successfully",
                request.mode()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Joins an existing game room.
     * POST /api/rooms/join
     */
    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(@RequestBody JoinRoomRequest request) {
        String playerId = roomService.joinRoom(request.gameId(), request.playerName());
        GameState game = gameEngine.getGame(request.gameId())
                .orElseThrow(() -> new GameNotFoundException(request.gameId()));
        
        GameStateUpdate update = new GameStateUpdate(
                game,
                request.playerName() + " joined the game",
                GameStateUpdate.UpdateType.PLAYER_JOINED
        );
        messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), update);
        
        RoomResponse response = new RoomResponse(
                request.gameId(),
                playerId,
                game,
                "Joined room successfully",
                game.getMode()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Starts a game.
     * POST /api/rooms/{gameId}/start
     */
    @PostMapping("/{gameId}/start")
    public ResponseEntity<RoomResponse> startGame(@PathVariable String gameId) {
        roomService.startGame(gameId);
        GameState game = gameEngine.getGame(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        
        GameStateUpdate update = new GameStateUpdate(
                game,
                "Game started!",
                GameStateUpdate.UpdateType.GAME_STARTED
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
        
        aiBotService.checkAndMakeBotMove(gameId);
        
        RoomResponse response = new RoomResponse(
                gameId,
                null,
                game,
                "Game started successfully",
                game.getMode()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets the current state of a game.
     * GET /api/rooms/{gameId}
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameState> getGameState(@PathVariable String gameId) {
        return gameEngine.getGame(gameId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lists all active games.
     * GET /api/rooms
     */
    @GetMapping
    public ResponseEntity<Collection<GameState>> listGames() {
        return ResponseEntity.ok(gameEngine.getAllGames());
    }
    
    /**
     * Leaves a game room.
     * POST /api/rooms/{gameId}/leave
     */
    @PostMapping("/{gameId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String gameId, @RequestBody LeaveRoomRequest request) {
        if (request.gameId() != null && !request.gameId().equals(gameId)) {
            return ResponseEntity.badRequest().build();
        }
        
        roomService.leaveRoom(gameId, request.playerId());
        
        messagingTemplate.convertAndSend("/topic/game/" + gameId, 
                new GameStateUpdate(null, "Player left", GameStateUpdate.UpdateType.PLAYER_LEFT));
        
        return ResponseEntity.ok().build();
    }
}
