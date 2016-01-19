package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.widget.Toast;

/**
 * Class to support app wide helper function
 * Created by danny on 10.01.2016.
 */
public class UIHelper {

    /**
     * show dialog with one button
     * @param activity
     * @param headline
     * @param message
     * @param button
     */
    public static void showDialogMessage(Activity activity, int headline, int message, int button) {

        //@todo: dummy function. replace it later with real dialog
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
