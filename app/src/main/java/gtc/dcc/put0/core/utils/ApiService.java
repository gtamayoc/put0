package gtc.dcc.put0.core.utils;

import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("user")
    Call<ResponseDetails> saveUser(@Body User user);

    @GET("user")
    Call<ResponseDetails> getAllUsers();

    @GET("user/{id}")
    Call<ResponseDetails> getUserById(@Path("id") int id);

    @GET("user/email/{email}")
    Call<ResponseDetails> getUserByEmail(@Path("email") String email);

    @POST("auth/login")
    Call<ResponseDetails> login(@Body User userReq);

}