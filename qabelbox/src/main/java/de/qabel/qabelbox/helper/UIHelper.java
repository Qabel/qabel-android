package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.qabel.qabelbox.R;

/**
 * Class to support app wide helper function
 * Created by danny on 10.01.2016.
 */
public class UIHelper {

    /**
     * show dialog with one button
     */
    public static void showDialogMessage(Activity activity, int headline, int message, int buttonOk, int buttonCancel, DialogInterface.OnClickListener buttonOkListener, DialogInterface.OnClickListener buttonCancelListener) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);
        builder.setTitle(headline);
        builder.setMessage(message);
        builder.setPositiveButton(buttonOk, buttonOkListener);
        if (buttonCancel != Integer.MIN_VALUE) {
            builder.setPositiveButton(buttonCancel, buttonCancelListener);
        }
        final AlertDialog dialog = builder.create();
        final FontHelper fontHelper = FontHelper.getInstance(activity);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog1) {
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.message));
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.title));

                fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
            }
        });

        dialog.show();
    }

    public static void showDialogMessage(Activity activity, int headline, int message) {

        showDialogMessage(activity, headline, message, R.string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, int headline, int message, DialogInterface.OnClickListener buttonOkListener) {

        showDialogMessage(activity, headline, message, R.string.ok, Integer.MIN_VALUE, buttonOkListener, null);
    }

    public static void showDialogMessage(Activity activity, int headline, int message, int buttonOk) {

        showDialogMessage(activity, headline, message, buttonOk, Integer.MIN_VALUE, null, null);
    }

    public static void showFunctionNotYetImplemented(Activity activity) {
        showDialogMessage(activity, R.string.dialog_headline_info, R.string.function_not_yet_implenented);
    }

    /**
     * show wait message
     */
    public static AlertDialog showWaitMessage(final Activity activity, int headline, int message, boolean cancelable) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(headline);
        builder.setMessage(message);

        builder.setCancelable(cancelable);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog1) {
                final FontHelper fontHelper = FontHelper.getInstance(activity);
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.message));
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.title));

            }
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);
        return dialog;
    }
}
