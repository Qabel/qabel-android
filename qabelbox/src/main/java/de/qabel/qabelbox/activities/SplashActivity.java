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

    public static final String START_MAIN = "START_MAIN";
    private final long LONG_SPLASH_TIME = 1500;
    private final long SHORT_SPLASH_TIME = 200;
    private SplashActivity mActivity;
    private AppPreference prefs;
    final private String TAG = this.getClass().getSimpleName();

    private boolean start_main;
    private boolean welcomeScreenShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent splashIntent = getIntent();
        start_main = splashIntent.getBooleanExtra(START_MAIN, true);
        mActivity = this;
        setupAppPreferences();
        welcomeScreenShown = prefs.getWelcomeScreenShownAt() == 0;
        if (welcomeScreenShown) {
            Intent intent = new Intent(mActivity, WelcomeScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                //@todo show whatsnew screen
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

        new Handler().postDelayed(() -> {
            if (!start_main) {
                return;
            }

            if (!welcomeScreenShown) {
                Intent intent = new Intent(mActivity, WelcomeScreenActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } else if (!Sanity.startWizardActivities(mActivity)) {

                Intent intent = new Intent(mActivity, MainActivity.class);
                intent.setAction("");
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }
                ,welcomeScreenShown ? SHORT_SPLASH_TIME : LONG_SPLASH_TIME);
    }
}
