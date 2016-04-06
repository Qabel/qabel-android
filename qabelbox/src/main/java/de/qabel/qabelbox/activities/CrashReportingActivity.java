package de.qabel.qabelbox.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.SettingsFragment;

/**
 * Created by danny on 05.02.16.
 */
public class CrashReportingActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private boolean handleCrashes = false;
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

    @Override
    public void onResume() {

        super.onResume();

        if (shouldHandleCrashes()) {
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

    public static boolean isDebugBuild(Context context) {
        String name = context.getPackageName();
        return name != null && name.endsWith(".debug");
    }
    private boolean shouldHandleCrashes() {
        return handleCrashes || !isDebugBuild(getApplicationContext());
    }
}
