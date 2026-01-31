package gtc.dcc.put0.core.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import gtc.dcc.put0.core.utils.CoreLogger;

import gtc.dcc.put0.R;
import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.utils.DialogUtils;
import gtc.dcc.put0.core.utils.NavigationUtils;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.viewmodel.UserViewModel;
import gtc.dcc.put0.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    UserViewModel userViewModel;

    private GoogleSignInClient mainActivity;
    private FirebaseAuth mAuth;
    private User user = new User();
    private androidx.activity.result.ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadCompatibility();
        // Initialize launcher before creation is complete
        googleSignInLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            // Google Sign In was successful, authenticate with Firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            CoreLogger.d("LoginActivity firebaseAuthWithGoogle:" + account.getId());
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            // Google Sign In failed, update UI appropriately
                            CoreLogger.d("LoginActivity Google sign in failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });

        loadComponents();
        loadViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferenceManager.clearToken();
        // signOut();
        CoreLogger.d("LoginActivity onResume Login ");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            return;

        CoreLogger.d("LoginActivity updateUI " + currentUser.getDisplayName());
        NavigationUtils.navigateToNext(this, MainActivity.class);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CoreLogger.d("LoginActivity onPause Login");
    }

    private void signIn() {
        Intent signInIntent = mainActivity.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        mainActivity.signOut().addOnCompleteListener(this, task -> {
            // Usuario desconectado
            CoreLogger.d("LoginActivity Se desconecto");
        }).addOnFailureListener(this, task -> {
            // Usuario desconectado
            CoreLogger.d("LoginActivity Usuario no conectado");
        });
    }

    private void loadViewModel() {
        userViewModel.getResponseDetailsLoginGoogle().observe(this, response -> {
            if (response != null && response.getData() != null) {
                handleSuccessfulLogin();
            } else if (response != null && response.getMessage() != null) {
                handleLoginError(response.getMessage());
            } else {
                signOut();
                Toast.makeText(this, "Service ERROR.", Toast.LENGTH_LONG).show();
                CoreLogger.d("Response is null.");
            }
        });

        userViewModel.getResponseDetailsRegisterGoogle().observe(this, response -> {
            if (response != null && response.getData() != null) {
                handleSuccessfulRegistration();
            } else if (response != null && response.getMessage() != null) {
                handleRegistrationError(response.getMessage());
            } else {
                signOut();
                Toast.makeText(this, "General error RegisterGoogle.", Toast.LENGTH_LONG).show();
                CoreLogger.d("Response is null.");
            }
        });

        userViewModel.getResponseDetailsLogin().observe(this, response -> {
            if (response != null && response.getData() != null) {
                handleSuccessfulLogin();
            } else {
                signOut();
                handleLoginFailure(response);
            }
        });
        binding.loginButtonGoogle.setOnClickListener(v -> {
            signIn();
        });
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            binding.appNameVersion.setText("V_" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadComponents() {
        // ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        CoreLogger.d("LoginActivity Google");
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mainActivity = GoogleSignIn.getClient(this, gso);
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            CoreLogger.d("LoginActivity Google Exception " + e.getMessage());
        }

    }

    private void loadCompatibility() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Inflate the layout using ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void updateUI(FirebaseUser currentUser) {
        CoreLogger.d("LoginActivity updateUI ");
        // ...
        if (currentUser == null)
            return;

        CoreLogger.d("LoginActivity updateUI " + currentUser.getDisplayName());
        NavigationUtils.navigateToNext(this, MainActivity.class);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    CoreLogger.d("LoginActivity signInWithCredential:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    completeUserRegistration(user);
                } else {
                    // If sign in fails, display a message to the user.
                    CoreLogger.d("LoginActivity signInWithCredential:failure " + task.getException());
                    updateUI(null);
                }
            }
        });
    }

    private void completeUserRegistration(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            CoreLogger.d("FirebaseUser is null, cannot proceed.");
            Toast.makeText(this, "FirebaseUser is null, cannot proceed.", Toast.LENGTH_LONG).show();
            return;
        }

        // Save name and email to SharedPrefs immediately
        String displayName = firebaseUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            SharedPreferenceManager.saveString("user_name", displayName);
            CoreLogger.d("Saved user name from Google: " + displayName);
        }

        String email = firebaseUser.getEmail();
        if (email != null) {
            SharedPreferenceManager.saveString("user_email", email);
        }

        Toast.makeText(this, "Login con Google Valid.", Toast.LENGTH_LONG).show();

        NavigationUtils.navigateToNext(this, MainActivity.class);
    }

    // Métodos auxiliares para mejorar la organización y legibilidad
    private void handleSuccessfulLogin() {
        CoreLogger.d("Login successful! Token received.");
        Toast.makeText(this, "Autenticación exitosa", Toast.LENGTH_SHORT).show();
        NavigationUtils.navigateToNext(this, MainActivity.class);
        // Opcional: Almacenar token en SharedPreferences
    }

    private void handleLoginError(String message) {
        if ("Invalid username or password.".equals(message)) {
            User user = createFullUser();
            userViewModel.saveUserGoogle(user);
        } else {
            signOut();
            Toast.makeText(this, "Login fallido - " + message, Toast.LENGTH_LONG).show();
            CoreLogger.d("Login failed.");
        }
    }

    private User createFullUser() {
        User user = new User();
        user.setEmailAddress(this.user.getEmailAddress());
        user.setNames(this.user.getNames());
        user.setUserName(this.user.getUserName());
        user.setSurNames(this.user.getSurNames());
        user.setUserPassword(this.user.getUserPassword());
        user.setRol(this.user.getRol());
        return user;
    }

    private void handleLoginFailure(ResponseDetails response) {
        if (response != null && response.getMessage() != null) {
            Toast.makeText(this, "Login fallido - " + response.getMessage(), Toast.LENGTH_SHORT).show();
            CoreLogger.d("Login failed: " + response.getMessage());
        } else {
            CoreLogger.d("Login failed: Response is null or has no message.");
            Toast.makeText(this, "Login fallido", Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos auxiliares para mejorar la organización y legibilidad
    private void handleSuccessfulRegistration() {
        SharedPreferenceManager.clearToken();
        CoreLogger.d("Registration successful!");
        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();

        try {
            User user = createUser();
            userViewModel.login(user);
        } catch (NullPointerException e) {
            Toast.makeText(this, "Algo falló: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleRegistrationError(String message) {
        if ("Error : User with this email already exists.".equals(message)) {
            User user = createUser();
            userViewModel.login(user);
        } else {
            signOut();
            Toast.makeText(this, "Registro fallido - " + message, Toast.LENGTH_LONG).show();
            CoreLogger.d("Registration failed.");
        }
    }

    private User createUser() {
        User user = new User();
        user.setEmailAddress(this.user.getEmailAddress());
        user.setUserPassword(this.user.getUserPassword());
        SharedPreferenceManager.saveString("user_email", user.getEmailAddress());
        return user;
    }

    private void alertExit() {
        DialogUtils.showConfirmationDialog(
                this,
                "Confirmación de salida",
                "¿Estás seguro de que deseas salir de la aplicación?",
                "Salir",
                () -> {
                    signOut(); // Lógica personalizada
                    finishAndRemoveTask(); // Cierra la actividad
                    System.exit(0);
                },
                "Cancelar");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        alertExit();
    }

}