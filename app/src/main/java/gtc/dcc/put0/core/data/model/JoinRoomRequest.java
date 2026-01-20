package gtc.dcc.put0.core.data.model;

import com.google.gson.annotations.SerializedName;

public class JoinRoomRequest {
    @SerializedName("gameId")
    private String gameId;
    @SerializedName("playerName")
    private String playerName;

    public JoinRoomRequest(String gameId, String playerName) {
        this.gameId = gameId;
        this.playerName = playerName;
    }
}
