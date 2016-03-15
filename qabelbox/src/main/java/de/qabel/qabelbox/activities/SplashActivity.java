package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.Sanity;

/**
 * Created by danny on 11.01.2016.
 */
public class SplashActivity extends CrashReportingActivity {

	private final long SPLASH_TIME = 1500;
	private SplashActivity mActivity;
	private AppPreference prefs;
	final private String TAG = this.getClass().getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		mActivity = this;
		setupAppPreferences();
		if (prefs.getWelcomeScreenShownAt() == 0) {
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
			Log.e(TAG,"error setting up preferences", e);
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

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {

                if (prefs.getWelcomeScreenShownAt() == 0) {
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
		}
				, SPLASH_TIME);
	}
}
