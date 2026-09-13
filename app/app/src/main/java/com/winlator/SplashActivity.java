package com.winlator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Brand boot screen for Mushrea.PC.
 *
 * <p>It plays a short, self-contained "system boot" sequence - a point of light that
 * expands into a glow, the logo resolving out of it, then the wordmark - before handing
 * over to the existing {@link MainActivity}. It is purely additive: it only starts
 * MainActivity and never touches the app's architecture, layouts or flows.
 *
 * <p>The sequence is driven by the native {@code ViewPropertyAnimator} (no GIF, video,
 * Lottie or extra dependency), is capped at ~2 seconds so it never blocks the user, and
 * is skipped entirely if the activity is relaunched on top of an existing task.
 */
public class SplashActivity extends AppCompatActivity {
    private static final long LAUNCH_AT_MILLIS = 2050;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean launched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this task already has an activity beneath us (e.g. returning from a
        // notification or the launcher relaunching the root), skip the splash instead of
        // showing it again.
        if (!isTaskRoot()) {
            finish();
            proceed();
            return;
        }

        setContentView(R.layout.splash_activity);

        final View glow = findViewById(R.id.splashGlow);
        final View pulse = findViewById(R.id.splashPulse);
        final ImageView logo = findViewById(R.id.splashLogo);
        final View wordmark = findViewById(R.id.splashWordmark);

        glow.setScaleX(0.6f); glow.setScaleY(0.6f);
        logo.setScaleX(0.85f); logo.setScaleY(0.85f);
        pulse.setScaleX(0.4f); pulse.setScaleY(0.4f);
        wordmark.setTranslationY(24f);

        // Stage 1: a small point of light appears.
        pulse.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(100).setDuration(200).start();

        // Stage 2: the light expands away as the glow blooms in behind the logo.
        handler.postDelayed(() -> {
            pulse.animate().scaleX(3.2f).scaleY(3.2f).alpha(0f).setDuration(450).start();
            glow.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start();
        }, 350);

        // Stage 3: the logo resolves out of the glow.
        handler.postDelayed(() ->
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start(), 650);

        // Stage 4: the wordmark settles in.
        handler.postDelayed(() ->
            wordmark.animate().alpha(1f).translationY(0f).setDuration(400).start(), 1050);

        // Stage 5: hand over to the real app.
        handler.postDelayed(this::proceed, LAUNCH_AT_MILLIS);
    }

    private void proceed() {
        if (launched) return;
        launched = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Back during the boot sequence exits the app cleanly; it never loops the splash.
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
