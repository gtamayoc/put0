package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;

public class RoomResponse {
    @SerializedName("gameId")
    private String gameId;
    @SerializedName("playerId")
    private String playerId;
    @SerializedName("gameState")
    private GameState gameState;
    @SerializedName("message")
    private String message;

    public String getGameId() {
        return gameId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public GameState getGameState() {
        return gameState;
    }

    public String getMessage() {
        return message;
    }
}
