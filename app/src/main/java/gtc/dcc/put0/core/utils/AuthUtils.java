package gtc.dcc.put0.core.utils;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import gtc.dcc.put0.core.utils.CoreLogger;

public final class AuthUtils {
    private AuthUtils() {
    }

    private static GoogleSignInClient googleSignInClient;
    private static FirebaseAuth firebaseAuth;

    public static void initializeGoogleSignIn(Context context, String webClientId) {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
            firebaseAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            CoreLogger.d("Google Sign-In initialization failed: " + e.getMessage());
        }
    }

    public static void signOut(Activity activity, Class<?> nextActivity) {
        firebaseAuth.signOut();
        SharedPreferenceManager.clearToken();
        googleSignInClient.signOut()
                .addOnCompleteListener(activity, task -> CoreLogger.d("User signed out successfully"));
        NavigationUtils.navigateToNext(activity, nextActivity);
    }
}