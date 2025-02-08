package gtc.dcc.put0.core.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.orhanobut.logger.Logger;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.utils.AuthUtils;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.viewmodel.MainViewModel;
import gtc.dcc.put0.databinding.ActivityAccountBinding;

public class AccountActivity extends AppCompatActivity {


    private MainViewModel viewModel;
    private ActivityAccountBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeViewModels();
        setupObservers();
        setupClickListeners();
        AuthUtils.initializeGoogleSignIn(this, getString(R.string.default_web_client_id));
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }

    private void setupObservers() {

    }

    private void setupClickListeners() {
    }

    private void updateUserInfo(String email, String imageUrl) {
//        Glide.with(this).load(imageUrl).into(binding.profileLogo);
        Logger.d("User email: " + email);
    }

    private void handleUserResponse(ResponseDetails response) {
        if (response == null) {
            handleSignOutError();
            return;
        }

        if (response.getData() instanceof User) {
            User user = (User) response.getData();
            saveUserData(user);
        }
    }

    private void handleSignOutError() {
        //AuthUtils.signOut(this, LoginActivity.class);
        Toast.makeText(this, "General error RegisterGoogle.", Toast.LENGTH_LONG).show();
    }

    private void saveUserData(User user) {
        SharedPreferenceManager.saveData("user_id", user.getId());
        SharedPreferenceManager.saveData("user_rol", user.getRol());
        SharedPreferenceManager.saveData("user_name", user.getNames());
    }

    private void showExitDialog() {
        DialogUtils.showExitConfirmationDialog(this, () -> AuthUtils.signOut(this, LoginActivity.class));
    }
}