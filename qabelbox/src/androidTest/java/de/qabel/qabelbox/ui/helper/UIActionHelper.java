package de.qabel.qabelbox.ui.helper;

import android.app.Activity;
import android.content.Intent;

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
}
