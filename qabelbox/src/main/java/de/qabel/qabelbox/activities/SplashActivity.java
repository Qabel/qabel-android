package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.Sanity;

public class SplashActivity extends CrashReportingActivity {

    private final long LONG_SPLASH_TIME = 1500;
    private final long SHORT_SPLASH_TIME = 200;

    public static String SKIP_SPLASH = "SKIP_SPLASH";

    private AppPreference prefs;
    private boolean skipSplash;
    final private String TAG = this.getClass().getSimpleName();

    private boolean welcomeScreenShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent splashIntent = getIntent();
        skipSplash = splashIntent.getBooleanExtra(SKIP_SPLASH, false);
        setupAppPreferences();
        welcomeScreenShown = prefs.getWelcomeScreenShownAt() != 0;
        setContentView(R.layout.activity_splashscreen);
        setupAppPreferences();
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
        int lastAppStartVersion = prefs.getLastAppStartVersion();

        int currentAppVersionCode = 0;
        try {
            currentAppVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "error setting up preferences", e);
        }
        if (lastAppStartVersion == 0) {
            prefs.setLastAppStartVersion(currentAppVersionCode);
        } else {
            if (lastAppStartVersion != currentAppVersionCode) {
                prefs.setLastAppStartVersion(currentAppVersionCode);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startDelayedHandler();
        }
    }

    private void startDelayedHandler() {
        long waitTime = welcomeScreenShown ? SHORT_SPLASH_TIME : LONG_SPLASH_TIME;
        new Handler().postDelayed(() -> {
            if (!welcomeScreenShown) {
                launch(WelcomeScreenActivity.class);
            } else if (!Sanity.startWizardActivities(this)) {
                launch(MainActivity.class);
            }
        }
                ,skipSplash ? 0 : waitTime);
    }

    private void launch(Class activity) {
        Intent intent = new Intent(this, activity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
