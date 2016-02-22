package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.Sanity;

/**
 * Created by danny on 11.01.2016.
 */
public class WelcomeScreenActivity extends Activity {

    private final long SPLASH_TIME = 1500;
    private WelcomeScreenActivity mActivity;
    private AppPreference prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_welcomescreen);
        setupAppPreferences();
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startDelayedHandler();
        }
    }

    private void startDelayedHandler() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                startMainActivity();
            }

            private void startMainActivity() {

                if (!Sanity.startWizardActivities(mActivity)) {

                    Intent intent = new Intent(mActivity, MainActivity.class);
                    intent.setAction("");
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            }
        }
                , SPLASH_TIME);
    }
}
