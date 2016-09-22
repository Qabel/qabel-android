package de.qabel.qabelbox;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.test.runner.AndroidJUnitRunner;

import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.index.preferences.AndroidIndexPreferences;
import de.qabel.qabelbox.index.preferences.IndexPreferences;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.ON_AFTER_RELEASE;

public class QblJUnitRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }

    @Override
    public void onCreate(Bundle arguments) {
        CreateIdentityActivity.Companion.setFAKE_COMMUNICATION(true);
        arguments.putString("disableAnalytics", "true");
        arguments.putString("package", "de.qabel.qabelbox");

        IndexPreferences indexPreferences = new AndroidIndexPreferences(getContext());
        indexPreferences.setContactSyncEnabled(false);
        indexPreferences.setPhoneStatePermission(true);

        AccountHelper.SYNC_INTERVAL = 0;
        super.onCreate(arguments);
    }
    private PowerManager.WakeLock wakeLock;

    @SuppressLint("MissingPermission")
    @Override public void onStart() {
        String name = QblJUnitRunner.class.getName();
        Context app = getTargetContext().getApplicationContext();

        // Unlock the device so that the tests can input keystrokes.
        KeyguardManager keyguard = (KeyguardManager) app.getSystemService(KEYGUARD_SERVICE);
        keyguard.newKeyguardLock(name).disableKeyguard();
        // Wake up the screen.
        PowerManager power = (PowerManager) app.getSystemService(POWER_SERVICE);
        wakeLock = power.newWakeLock(FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE, name);
        wakeLock.acquire();

        super.onStart();
    }

    @Override public void onDestroy() {
        super.onDestroy();

        wakeLock.release();
    }
}
