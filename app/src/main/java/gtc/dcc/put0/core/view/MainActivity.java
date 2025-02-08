package gtc.dcc.put0.core.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.bottomsheets.BottomSheet;
import com.orhanobut.logger.Logger;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.utils.AuthUtils;
import gtc.dcc.put0.core.utils.CodeGenerator;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.NavigationUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.viewmodel.MainViewModel;
import gtc.dcc.put0.core.viewmodel.UserViewModel;
import gtc.dcc.put0.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {


    private MainViewModel viewModel;
    private UserViewModel userViewModel;
    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadCompatibility();
        initializeViewModels();
        setupObservers();
        setupClickListeners();
        AuthUtils.initializeGoogleSignIn(this, getString(R.string.default_web_client_id));
    }

    private void loadCompatibility() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initializeViewModels() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    private void setupObservers() {
        userViewModel.getResponseDetailsGetUser().observe(this, this::handleUserResponse);
        viewModel.selectedActivity.observe(this, activity -> {
            NavigationUtils.navigateToNextFinis(this, activity);
        });
    }

    private void setupClickListeners() {
        binding.btnCreateGame.setOnClickListener(v -> {
                    //new MaterialDialog(this, new BottomSheet()).show();

                    DialogUtils.showGameFormDialog(this, (gameName, minPlayers, maxPlayers) -> {
                        // Aquí procesas los resultados después de que el formulario se envíe
                        CodeGenerator generator = new CodeGenerator("GTS-@@@-###");
                        String gameCode = generator.generateCode();
                        Logger.d("Partida creada: " + gameName + " Código: " + gameCode);

                        int uniqueId = generator.generateUniqueId(gameName, gameCode);
                        Logger.d("Identificador único: " + uniqueId);
                    });
                }
        );
        binding.btnJoinGame.setOnClickListener(v -> {
                    viewModel.onJoinGameClicked();
                }
        );
        binding.btnAddFriends.setOnClickListener(v -> viewModel.onAddFriendsClicked());
        binding.btnSettings.setOnClickListener(v -> viewModel.onSettingsClicked());
        binding.btnLogout.setOnClickListener(v -> showExitDialog());
    }

    private void updateUserInfo(String email, String imageUrl) {
        userViewModel.getUserByEmail(email);
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