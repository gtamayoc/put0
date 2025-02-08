package gtc.dcc.put0.core.repository;

import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.utils.ApiService;
import gtc.dcc.put0.core.utils.CallbackHandler;
import gtc.dcc.put0.core.utils.RetrofitClient;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;

    public UserRepository() {
        apiService = RetrofitClient.getInstance().create(ApiService.class);
    }


    public void login(User userReq, CallbackHandler<ResponseDetails> callbackHandler) {
        apiService.login(userReq).enqueue(new Callback<ResponseDetails>() {
            @Override
            public void onResponse(Call<ResponseDetails> call, Response<ResponseDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = (String) response.body().getData();
                    SharedPreferenceManager.setToken(token); // Guarda el token
                    callbackHandler.onSuccess(response.body());
                } else {
                    callbackHandler.onFailure(new Throwable("Error en la respuesta del servidor"));
                }
            }

            @Override
            public void onFailure(Call<ResponseDetails> call, Throwable t) {
                callbackHandler.onFailure(t);
            }
        });
    }

    public void saveUser(User user, CallbackHandler<ResponseDetails> callbackHandler) {
        apiService.saveUser(user).enqueue(new Callback<ResponseDetails>() {
            @Override
            public void onResponse(Call<ResponseDetails> call, Response<ResponseDetails> response) {
                callbackHandler.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ResponseDetails> call, Throwable t) {
                callbackHandler.onFailure(t);
            }
        });
    }


    public void getUserByEmail(String email, CallbackHandler<ResponseDetails> callbackHandler) {
        apiService.getUserByEmail(email).enqueue(new Callback<ResponseDetails>() {
            @Override
            public void onResponse(Call<ResponseDetails> call, Response<ResponseDetails> response) {
                callbackHandler.onSuccess(response.body());
            }

            @Override
            public void onFailure(Call<ResponseDetails> call, Throwable t) {
                callbackHandler.onFailure(t);
            }
        });
    }

    // Implementa los métodos `getAllUsers`, `getUserById`, etc.
}