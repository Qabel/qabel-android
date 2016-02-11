package de.qabel.qabelbox.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.PrefixServer;
import de.qabel.qabelbox.config.AppPreference;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

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

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_splashscreen);
        setupAppPreferences();
        testGetPrefix();
    }

    private void testGetPrefix() {

        new PrefixServer().getPrefix(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.d(TAG, "danny failed: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                int code = response.code();

                Log.d(TAG, "danny: " + response.toString() + " " + response.code() + " " + call.request().toString() + " " + call.request().headers().toString());
                boolean error=true;
                if (code == 201) {
                    String text = response.body().toString();
                    try {
                        PrefixServer.ServerResponse result = PrefixServer.parseJson(new JSONObject(text));
                        Log.d(TAG, "prefix: " + result.prefix);
                        error=false;
                    } catch (JSONException e) {
                        Log.w(TAG, "error on parse service response", e);
                    }
                }
                if (code == 401) {
                    //{"detail":"Invalid token."}
                    //unauthorized
                } else {

                }
            }
        });
    }

    private void setupAppPreferences() {

        prefs = new AppPreference(this);
        int lastAppStartVersion = prefs.getLastAppStartVersion();

        int currentAppVersionCode = 0;
        try {
            currentAppVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (lastAppStartVersion == 0) {
            prefs.setLastAppStartVersion(currentAppVersionCode);
            //@todo show welcome
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

                startMainActivity();
            }

            private void startMainActivity() {

                if (!startWizardActivities(mActivity)) {

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

    /**
     * start wizard activities if app not ready to go. If other activity need to start, the current activity finished
     *
     * @param activity current activity
     * @return false if no wizard start needed
     */
    public static boolean startWizardActivities(Activity activity) {

        AppPreference prefs = new AppPreference(activity);

        if (prefs.getToken() == null) {
            Intent intent = new Intent(activity, CreateAccountActivity.class);
            intent.putExtra(BaseWizardActivity.FIRST_RUN, true);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            activity.finish();
            return true;
        } else if (QabelBoxApplication.getInstance().getService().getIdentities().getIdentities().size() == 0) {
            Intent intent = new Intent(activity, CreateIdentityActivity.class);
            intent.putExtra(BaseWizardActivity.FIRST_RUN, true);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            activity.finish();
            return true;
        } else {
            return false;
        }
    }
}
