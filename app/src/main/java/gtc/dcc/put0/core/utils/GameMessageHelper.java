package gtc.dcc.put0.core.utils;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import gtc.dcc.put0.R;

/**
 * Helper class for displaying game messages in a UX-friendly, non-invasive way.
 * 
 * Design Principles:
 * - Clear, brief, and non-invasive messages
 * - Maintains game flow without blocking UI
 * - Avoids blaming the player
 * - Uses neutral, empathetic tone
 * 
 * Memory Safety:
 * - Uses WeakReferences to prevent memory leaks
 * - Dismisses previous Snackbars to avoid ANRs
 * - Null-safe operations throughout
 */
public class GameMessageHelper {

    /**
     * Message types with associated visual styles.
     */
    public enum MessageType {
        SUCCESS, // Green, positive feedback
        INFO, // Blue, neutral information
        PHASE, // Yellow/Amber, phase transitions
        NEUTRAL // Gray, no emphasis
    }

    /**
     * Duration constants for messages.
     */
    public static final int DURATION_SHORT = 2000; // 2 seconds
    public static final int DURATION_MEDIUM = 3000; // 3 seconds
    public static final int DURATION_LONG = 4000; // 4 seconds

    // Keep weak reference to last Snackbar to dismiss it if needed
    private static WeakReference<Snackbar> lastSnackbarRef;

    /**
     * Shows a Snackbar message with default duration (2 seconds).
     * 
     * @param view    Root view to attach Snackbar to
     * @param message Message string resource ID
     * @param type    Message type for styling
     */
    public static void showMessage(@NonNull View view, @StringRes int message, @NonNull MessageType type) {
        showMessage(view, view.getContext().getString(message), type, DURATION_SHORT);
    }

    /**
     * Shows a Snackbar message with default duration (2 seconds).
     * 
     * @param view    Root view to attach Snackbar to
     * @param message Message string
     * @param type    Message type for styling
     */
    public static void showMessage(@NonNull View view, @NonNull String message, @NonNull MessageType type) {
        showMessage(view, message, type, DURATION_SHORT);
    }

    /**
     * Shows a Snackbar message with custom duration.
     * 
     * @param view       Root view to attach Snackbar to
     * @param message    Message string resource ID
     * @param type       Message type for styling
     * @param durationMs Duration in milliseconds
     */
    public static void showMessage(@NonNull View view, @StringRes int message, @NonNull MessageType type,
            int durationMs) {
        showMessage(view, view.getContext().getString(message), type, durationMs);
    }

    /**
     * Shows a Snackbar message with custom duration.
     * Memory-safe implementation that dismisses previous Snackbars.
     * 
     * @param view       Root view to attach Snackbar to
     * @param message    Message string
     * @param type       Message type for styling
     * @param durationMs Duration in milliseconds
     */
    public static void showMessage(@NonNull View view, @NonNull String message, @NonNull MessageType type,
            int durationMs) {
        if (view == null || message == null || message.isEmpty()) {
            return;
        }

        // Dismiss previous Snackbar to avoid stacking and ANRs
        dismissLastSnackbar();

        Context context = view.getContext();
        if (context == null) {
            return;
        }

        // Adjust duration based on message length (Amigable: give time to read)
        int finalDuration = durationMs;
        if (message.length() > 60) {
            finalDuration = Math.max(durationMs, DURATION_LONG);
        }
        if (message.length() > 120) {
            finalDuration = 6000; // 6 seconds for very long text
        }

        // Create Snackbar
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setDuration(finalDuration);

        // Apply styling based on message type
        applyMessageStyle(snackbar, type, context);

        // Store weak reference
        lastSnackbarRef = new WeakReference<>(snackbar);

        // Show the Snackbar
        snackbar.show();
    }

    /**
     * Shows a formatted Snackbar message with parameters.
     * 
     * @param view         Root view to attach Snackbar to
     * @param messageResId Message string resource ID with format specifiers
     * @param type         Message type for styling
     * @param formatArgs   Arguments for string formatting
     */
    public static void showFormattedMessage(@NonNull View view, @StringRes int messageResId,
            @NonNull MessageType type, Object... formatArgs) {
        if (view == null || view.getContext() == null) {
            return;
        }

        String message = view.getContext().getString(messageResId, formatArgs);
        showMessage(view, message, type, DURATION_SHORT);
    }

    /**
     * Applies visual styling to Snackbar based on message type.
     * Uses custom layout for better typography and visual design.
     */
    private static void applyMessageStyle(
            @NonNull Snackbar snackbar,
            @NonNull MessageType type,
            @NonNull Context context
    ) {
        // Get Snackbar root view (NO SnackbarLayout)
        View snackbarView = snackbar.getView();
        ViewGroup snackbarContainer = (ViewGroup) snackbarView;

        // Transparent base
        snackbarView.setBackgroundColor(Color.TRANSPARENT);
        snackbarView.setPadding(0, 0, 0, 0);
        snackbarView.setElevation(dpToPx(context, 8));

        // Inflate custom layout
        View customView = LayoutInflater.from(context)
                .inflate(R.layout.custom_snackbar_layout, snackbarContainer, false);

        // Custom layout views
        TextView textView = customView.findViewById(R.id.snackbar_text);
        ImageView iconView = customView.findViewById(R.id.snackbar_icon);

        // Extract original Snackbar message
        TextView originalTextView =
                snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        String messageText =
                originalTextView != null ? originalTextView.getText().toString() : "";

        textView.setText(messageText);

        // Text alignment
        textView.setTextAlignment(
                messageText.length() > 40
                        ? View.TEXT_ALIGNMENT_TEXT_START
                        : View.TEXT_ALIGNMENT_CENTER
        );

        // Style by type
        int backgroundColor;
        int iconResId;

        switch (type) {
            case SUCCESS:
                backgroundColor = ContextCompat.getColor(context, R.color.msg_success);
                iconResId = android.R.drawable.ic_menu_info_details;
                break;
            case INFO:
                backgroundColor = ContextCompat.getColor(context, R.color.msg_info);
                iconResId = android.R.drawable.ic_dialog_info;
                break;
            case PHASE:
                backgroundColor = ContextCompat.getColor(context, R.color.msg_phase);
                iconResId = android.R.drawable.ic_menu_rotate;
                break;
            case NEUTRAL:
            default:
                backgroundColor = ContextCompat.getColor(context, R.color.msg_neutral);
                iconResId = 0;
                break;
        }

        // Background handling
        Drawable background = customView.getBackground();
        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background).setColor(backgroundColor);
        } else if (background != null) {
            background.setTint(backgroundColor);
        } else {
            customView.setBackgroundColor(backgroundColor);
        }

        // Icon
        if (iconResId != 0) {
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        // Remove default Snackbar content
        snackbarContainer.removeAllViews();
        snackbarContainer.addView(customView);

        // Positioning (TOP floating style)
        ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) lp;
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;

            int topMargin = dpToPx(context, 48);
            int sideMargin = dpToPx(context, 16);
            params.setMargins(sideMargin, topMargin, sideMargin, 0);

            if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity =
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            } else if (params instanceof CoordinatorLayout.LayoutParams) {
                ((CoordinatorLayout.LayoutParams) params).gravity =
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            }

            snackbarView.setLayoutParams(params);
        }

        // Entrance animation
        snackbarView.setAlpha(0f);
        snackbarView.setTranslationY(-dpToPx(context, 48));
        snackbarView.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator((TimeInterpolator) new DecelerateInterpolator())
                .start();
    }

    /**
     * Converts dp to pixels.
     */
    private static int dpToPx(@NonNull Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Dismisses the last shown Snackbar if it's still visible.
     * Prevents stacking of messages and potential ANRs.
     */
    private static void dismissLastSnackbar() {
        if (lastSnackbarRef != null) {
            Snackbar lastSnackbar = lastSnackbarRef.get();
            if (lastSnackbar != null && lastSnackbar.isShown()) {
                lastSnackbar.dismiss();
            }
            lastSnackbarRef = null;
        }
    }

    /**
     * Shows a Toast message (use sparingly, only for critical system errors).
     * 
     * @param context Context
     * @param message Message string resource ID
     */
    public static void showToast(@NonNull Context context, @StringRes int message) {
        if (context == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a Toast message (use sparingly, only for critical system errors).
     * 
     * @param context Context
     * @param message Message string
     */
    public static void showToast(@NonNull Context context, @NonNull String message) {
        if (context == null || message == null || message.isEmpty()) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Plays a Lottie animation for visual feedback (no text).
     * Memory-safe: uses post() to avoid ANRs.
     * 
     * @param animationView LottieAnimationView to animate
     * @param rawResId      Raw resource ID of the animation
     */
    public static void showAnimation(@NonNull LottieAnimationView animationView, int rawResId) {
        if (animationView == null) {
            return;
        }

        // Use post() to ensure we're on the UI thread and avoid ANRs
        animationView.post(() -> {
            animationView.setAnimation(rawResId);
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();

            // Auto-hide after animation completes
            animationView.addAnimatorListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    animationView.setVisibility(View.GONE);
                    animationView.removeAnimatorListener(this);
                }
            });
        });
    }

    /**
     * Cleans up resources. Call this when the activity/fragment is destroyed.
     */
    public static void cleanup() {
        dismissLastSnackbar();
    }
}
