package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.WindowManager;
import de.qabel.qabelbox.activities.MainActivity;

/**
 * Created by danny on 17.01.16.
 */
public class UIActionHelper {

    void goHome(Activity activity) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        activity.startActivity(intent);
    }

    void bringToForeground(Activity activity) {

        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    void startActivity(Activity context) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("");
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static PowerManager.WakeLock wakeupDevice(final Activity activity) {

        KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("Unlock!");
        keyguardLock.disableKeyguard();


        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "Wakeup!");
        wakeLock.acquire();
        UITestHelper.sleep(500);
        //wait until display is unlocked
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            }
        });
        UITestHelper.sleep(500);

        return wakeLock;
    }
}
