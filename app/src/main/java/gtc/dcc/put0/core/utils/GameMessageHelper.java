package gtc.dcc.put0.core.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
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

        // Create Snackbar
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        // Set custom duration
        snackbar.setDuration(durationMs);

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
     * 
     * @param snackbar Snackbar to style
     * @param type     Message type
     * @param context  Context for resolving colors
     */
    private static void applyMessageStyle(@NonNull Snackbar snackbar, @NonNull MessageType type,
            @NonNull Context context) {
        // Get Snackbar's layout
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        // Remove default padding to use custom layout
        snackbarLayout.setPadding(0, 0, 0, 0);

        // Inflate custom layout
        View customView = LayoutInflater.from(context)
                .inflate(R.layout.custom_snackbar_layout, null);

        // Get views from custom layout
        TextView textView = customView.findViewById(R.id.snackbar_text);
        ImageView iconView = customView.findViewById(R.id.snackbar_icon);

        // Get the original Snackbar TextView to extract the message
        TextView originalTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        CharSequence messageText = originalTextView != null ? originalTextView.getText() : "";

        // Set message text to custom view
        textView.setText(messageText);

        // Configure background color and icon based on type
        int backgroundColor;
        int iconResId = 0;

        switch (type) {
            case SUCCESS:
                backgroundColor = ContextCompat.getColor(context, R.color.msg_success);
                iconResId = android.R.drawable.ic_menu_info_details; // Checkmark icon
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
                iconResId = 0; // No icon for neutral
                break;
        }

        // Apply background color to custom view
        GradientDrawable background = (GradientDrawable) customView.getBackground();
        if (background != null) {
            background.setColor(backgroundColor);
        } else {
            customView.setBackgroundColor(backgroundColor);
        }

        // Show/hide icon
        if (iconResId != 0) {
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        // Remove default TextView
        ((ViewGroup) snackbarLayout.getChildAt(0)).removeAllViews();

        // Add custom view
        snackbarLayout.addView(customView, 0);

        // Configure Snackbar positioning (Notification Style - TOP, Centered, Wrap
        // Content)
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarLayout.getLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Allow it to shrink to fit content

        // Calculate top margin (adjust for status bar if needed, usually handled by
        // fitSystemWindows)
        // We want it to "float" a bit from the top edge
        int topMargin = dpToPx(context, 48); // Lower it below status bar/toolbar area

        // Add margins for floating card effect
        params.setMargins(
                dpToPx(context, 16), // left
                topMargin, // top (Start from top)
                dpToPx(context, 16), // right
                0 // bottom
        );

        // Force Gravity to TOP
        if (params instanceof android.widget.FrameLayout.LayoutParams) {
            ((android.widget.FrameLayout.LayoutParams) params).gravity = android.view.Gravity.TOP
                    | android.view.Gravity.CENTER_HORIZONTAL;
        } else if (params instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
            ((androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) params).gravity = android.view.Gravity.TOP
                    | android.view.Gravity.CENTER_HORIZONTAL;
        }

        // Make corners rounded and ensure elevation
        snackbarLayout.setLayoutParams(params);

        // Actually default animation slides from bottom. For top, we might need a
        // custom animation or just accept the fade.
        // For simplicity and efficiency, we rely on the layout jumping to top.
        // Snackbar doesn't natively support slide-down.
        // We can add a simple translation animation on the view itself.

        snackbarLayout.setTranslationY(-dpToPx(context, 100)); // Start off-screen top
        snackbarLayout.animate().translationY(0).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        snackbarLayout.setElevation(dpToPx(context, 8));

        // Ensure background is transparent (custom view handles background)
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
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
