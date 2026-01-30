package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateRoomRequest {
    @SerializedName("playerName")
    private String playerName;
    @SerializedName("isPrivate")
    private boolean isPrivate;
    @SerializedName("maxPlayers")
    private int maxPlayers;
    @SerializedName("botCount")
    private int botCount;

    public CreateRoomRequest(String playerName, int botCount, MatchMode mode, boolean isPrivate, int maxPlayers) {
        this.playerName = playerName;
        this.botCount = botCount;
        this.mode = mode;
        this.isPrivate = isPrivate;
        this.maxPlayers = maxPlayers;
    }

    @SerializedName("mode")
    private MatchMode mode;
}
