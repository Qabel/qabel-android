package de.qabel.qabelbox.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.SettingsFragment;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

/**
 * Created by danny on 05.02.16.
 */
public class CrashReportingActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private boolean handleCrashes = true;
    private final boolean checkForUpdates = false;//set to true if certain users can uploadAndDeleteLocalfile new version via hockey app

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (checkForUpdates) {
            // Remove this for store / production builds!
            UpdateManager.register(this, getString(R.string.hockeykey));
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (checkForUpdates) {
            UpdateManager.unregister();
        }
    }

    /**
     * enable or disable the bug tracking for the current activity
     *
     * @param handleCrashes set to true if crash tracking should be on
     */
    public void enableCrashHandling(boolean handleCrashes) {

        this.handleCrashes = handleCrashes;
    }

    @Override
    public void onResume() {

        super.onResume();

        if (handleCrashes) {
            SharedPreferences preferences = getSharedPreferences(
                    SettingsFragment.APP_PREF_NAME,
                    Context.MODE_PRIVATE);
            if (preferences.getBoolean(getString(R.string.settings_key_bugreporting_enabled), true)) {
                Log.v(TAG, "install crash reporting handler");
                CrashManager.register(this, getString(R.string.hockeykey));
            } else {
                Log.d(TAG, "crash reporting DISABLED");
            }
        } else {
            Log.v(TAG, "crash reporting handler deactivated");
        }
    }
}
