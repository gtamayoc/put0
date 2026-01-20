package gtc.dcc.put0.core.data.remote;

import gtc.dcc.put0.core.data.model.CreateRoomRequest;
import gtc.dcc.put0.core.data.model.GameState;
import gtc.dcc.put0.core.data.model.JoinRoomRequest;
import gtc.dcc.put0.core.data.model.RoomResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import java.util.List;

public interface Put0ApiService {
    @POST("api/rooms/create")
    Call<RoomResponse> createRoom(@Body CreateRoomRequest request);

    @POST("api/rooms/join")
    Call<RoomResponse> joinRoom(@Body JoinRoomRequest request);

    @POST("api/rooms/{gameId}/start")
    Call<RoomResponse> startGame(@Path("gameId") String gameId);

    @GET("api/rooms/{gameId}")
    Call<GameState> getGameState(@Path("gameId") String gameId);

    @POST("api/rooms/{gameId}/leave")
    Call<Void> leaveRoom(@Path("gameId") String gameId, @Body JoinRoomRequest request);
}
