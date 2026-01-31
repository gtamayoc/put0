package com.game.server.put0.controller;

import com.game.server.put0.dto.*;
import com.game.core.model.GameState;
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
        try {
            if (request.getMode() == null) {
                return ResponseEntity.badRequest()
                        .body(new RoomResponse(null, null, null, "Match Mode is required", null));
            }
            
            RoomService.RoomCreationResult result = roomService.createRoom(
                    request.getPlayerName(),
                    request.getBotCount(),
                    request.getMode()
            );
            
            GameState game = gameEngine.getGame(result.gameId());
            
            RoomResponse response = new RoomResponse(
                    result.gameId(),
                    result.playerId(),
                    game,
                    "Room created successfully",
                    request.getMode()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating room: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new RoomResponse(null, null, null, e.getMessage(), null));
        }
    }
    
    /**
     * Joins an existing game room.
     * POST /api/rooms/join
     */
    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(@RequestBody JoinRoomRequest request) {
        try {
            String playerId = roomService.joinRoom(request.getGameId(), request.getPlayerName());
            GameState game = gameEngine.getGame(request.getGameId());
            
            // Notify other players
            GameStateUpdate update = new GameStateUpdate(
                    game,
                    request.getPlayerName() + " joined the game",
                    GameStateUpdate.UpdateType.PLAYER_JOINED
            );
            messagingTemplate.convertAndSend("/topic/game/" + request.getGameId(), update);
            
            RoomResponse response = new RoomResponse(
                    request.getGameId(),
                    playerId,
                    game,
                    "Joined room successfully",
                    game.getMode()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error joining room: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new RoomResponse(null, null, null, e.getMessage(), null));
        }
    }
    
    /**
     * Starts a game.
     * POST /api/rooms/{gameId}/start
     */
    @PostMapping("/{gameId}/start")
    public ResponseEntity<RoomResponse> startGame(@PathVariable String gameId) {
        try {
            roomService.startGame(gameId);
            GameState game = gameEngine.getGame(gameId);
            
            // Notify all players
            GameStateUpdate update = new GameStateUpdate(
                    game,
                    "Game started!",
                    GameStateUpdate.UpdateType.GAME_STARTED
            );
            messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
            
            // If first player is a bot, make its move
            aiBotService.checkAndMakeBotMove(gameId);
            
            RoomResponse response = new RoomResponse(
                    gameId,
                    null,
                    game,
                    "Game started successfully",
                    game.getMode()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting game: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new RoomResponse(null, null, null, e.getMessage(), null));
        }
    }
    
    /**
     * Gets the current state of a game.
     * GET /api/rooms/{gameId}
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameState> getGameState(@PathVariable String gameId) {
        GameState game = gameEngine.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
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
        try {
            // Validate that path variable matches body if body contains gameId
            if (request.getGameId() != null && !request.getGameId().equals(gameId)) {
                return ResponseEntity.badRequest().build();
            }
            
            roomService.leaveRoom(gameId, request.getPlayerId());
            
            // Notify others
            messagingTemplate.convertAndSend("/topic/game/" + gameId, new GameStateUpdate(null, "Player left", GameStateUpdate.UpdateType.PLAYER_LEFT));
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error leaving room: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
