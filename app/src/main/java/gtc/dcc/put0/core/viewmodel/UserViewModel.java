package gtc.dcc.put0.core.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import gtc.dcc.put0.core.model.ResponseDetails;
import gtc.dcc.put0.core.model.Rol;
import gtc.dcc.put0.core.model.User;
import gtc.dcc.put0.core.repository.UserRepository;
import gtc.dcc.put0.core.utils.CallbackHandler;


public class UserViewModel extends ViewModel {

    private final UserRepository repository;

    private final MutableLiveData<ResponseDetails> response_details_register = new MutableLiveData<>();
    private LiveData<ResponseDetails> responseDetailsRegister = response_details_register;
    private final MutableLiveData<ResponseDetails> response_details_register_google = new MutableLiveData<>();
    private LiveData<ResponseDetails> responseDetailsRegisterGoogle = response_details_register_google;
    private final MutableLiveData<ResponseDetails> response_details_login_google = new MutableLiveData<>();
    private LiveData<ResponseDetails> responseDetailsLoginGoogle = response_details_login_google;
    private final MutableLiveData<ResponseDetails> response_details_login = new MutableLiveData<>();
    private LiveData<ResponseDetails> responseDetailsLogin = response_details_login;
    private final MutableLiveData<ResponseDetails> response_details_get_user = new MutableLiveData<>();
    private LiveData<ResponseDetails> responseDetailsGetUser = response_details_get_user;

    private final MutableLiveData<String> _loginErrorResult = new MutableLiveData<>();
    private LiveData<String> loginErrorResult = _loginErrorResult;

    private final MutableLiveData<String> _loginResult = new MutableLiveData<>();
    private LiveData<String> loginResult = _loginResult;

    public UserViewModel() {
        repository = new UserRepository();
    }

    public LiveData<String> getLoginErrorResult() {
        return loginErrorResult;
    }

    public LiveData<String> getLoginResult() {
        return loginResult;
    }

    public LiveData<ResponseDetails> getResponseDetailsRegister() {
        return responseDetailsRegister;
    }

    public LiveData<ResponseDetails> getResponseDetailsRegisterGoogle() {
        return responseDetailsRegisterGoogle;
    }

    public LiveData<ResponseDetails> getResponseDetailsLoginGoogle() {
        return responseDetailsLoginGoogle;
    }

    public LiveData<ResponseDetails> getResponseDetailsLogin() {
        return responseDetailsLogin;
    }

    public LiveData<ResponseDetails> getResponseDetailsGetUser() {
        return responseDetailsGetUser;
    }


    public void validarRegistro(String nombre, String apellido, String email, String password, String repeatPassword, Rol role, String usuario) {

        // Validar que los campos no sean null ni estén vacíos
        if (nombre == null || nombre.isEmpty() ||
                apellido == null || apellido.isEmpty() ||
                email == null || email.isEmpty() ||
                password == null || password.isEmpty() ||
                repeatPassword == null || repeatPassword.isEmpty() ||
                usuario == null || usuario.isEmpty()) {

            _loginErrorResult.setValue("Error: All fields are required.");
            return;
        }

        // Validación adicional para el rol del usuario
        if (role == null) {
            _loginErrorResult.setValue("Error: User role is required.");
            return;
        }

        // Validar formato del email
        if (!isValidEmail(email)) {
            _loginErrorResult.setValue("Error: Invalid email format.");
            return;
        }

        // Validar que las contraseñas coincidan
        if (!password.equals(repeatPassword)) {
            _loginErrorResult.setValue("Error: Passwords do not match.");
            return;
        }

        _loginResult.setValue("gone");
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Aquí se puede usar una expresión regular o librerías específicas para validar el formato de número de teléfono colombiano
        return phoneNumber.matches("\\+57\\d{10}"); // Ejemplo básico para números de teléfono colombianos
    }

    public void login(User user) {
        repository.login(user, new CallbackHandler<ResponseDetails>() {
            @Override
            public void onSuccess(ResponseDetails result) {
                response_details_login.setValue(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                response_details_login.setValue(null); // O maneja el error de otra forma
            }
        });
    }

    public void loginGoogle(User user) {
        repository.login(user, new CallbackHandler<ResponseDetails>() {
            @Override
            public void onSuccess(ResponseDetails result) {
                response_details_login_google.setValue(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                response_details_login_google.setValue(null); // O maneja el error de otra forma
            }
        });
    }

    public void saveUser(User user) {
        repository.saveUser(user, new CallbackHandler<ResponseDetails>() {
            @Override
            public void onSuccess(ResponseDetails result) {
                response_details_register.setValue(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                response_details_register.setValue(null); // O maneja el error de otra forma
            }
        });
    }

    public void saveUserGoogle(User user) {
        repository.saveUser(user, new CallbackHandler<ResponseDetails>() {
            @Override
            public void onSuccess(ResponseDetails result) {
                response_details_register_google.setValue(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                response_details_register_google.setValue(null); // O maneja el error de otra forma
            }
        });
    }

    public void getUserByEmail(String email) {
        repository.getUserByEmail(email, new CallbackHandler<ResponseDetails>() {
            @Override
            public void onSuccess(ResponseDetails result) {
                response_details_get_user.setValue(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                response_details_get_user.setValue(null); // O maneja el error de otra forma
            }
        });
    }
    // Implementa métodos para los otros endpoints
}
