package de.qabel.qabelbox.helper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.views.EditTextFont;

import java.io.IOException;

public class UIHelper {

    public static void showConfirmationDialog(final Activity activity, final int headline,
                                              final int message, int icon,
                                              final DialogInterface.OnClickListener buttonOkListener){
        showDialogMessage(activity, activity.getString(headline), activity.getString(message),
                R.string.ok, R.string.cancel, icon, buttonOkListener, ((dialog, which) -> dialog.cancel()));
    }

    /**
     * show dialog with one buttonhelp
     */
    public static void showDialogMessage(final Activity activity, final int headline, final int message, final int buttonOk, final int buttonCancel, final DialogInterface.OnClickListener buttonOkListener, final DialogInterface.OnClickListener buttonCancelListener) {

        showDialogMessage(activity, activity.getString(headline), activity.getString(message), buttonOk, buttonCancel, buttonOkListener, buttonCancelListener);
    }

    public static void showDialogMessage(final Activity activity, final String headline, final String message, final int buttonOk, final int buttonCancel, final DialogInterface.OnClickListener buttonOkListener, final DialogInterface.OnClickListener buttonCancelListener) {
        showDialogMessage(activity, headline, message, buttonOk, buttonCancel, null, buttonOkListener, buttonCancelListener);
    }
    /**
     * show dialog with one button
     */
    public static void showDialogMessage(final Activity activity, final String headline, final String message, final int buttonOk, final int buttonCancel, Integer icon, final DialogInterface.OnClickListener buttonOkListener, final DialogInterface.OnClickListener buttonCancelListener) {

        final AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);
        builder.setTitle(headline);
        builder.setMessage(message);
        if(icon != null){
            builder.setIcon(icon);
        }
        builder.setPositiveButton(buttonOk, buttonOkListener);
        if (buttonCancel != Integer.MIN_VALUE) {
            builder.setNegativeButton(buttonCancel, buttonCancelListener);
        }

        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(() -> {
            final AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dialog1 -> {

                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.message));
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.title));

                fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
            });

            dialog.show();
        });
    }

    public static void showDialogMessage(Activity activity, int headline, int message) {

        showDialogMessage(activity, headline, message, R.string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, int headline, String message) {

        showDialogMessage(activity, activity.getString(headline), message, R.string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, String headline, String message) {
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

                final FontHelper fontHelper = FontHelper.getInstance();
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

    public static void showEditTextDialog(Activity activity, int title, int message, int ok, int cancel, final EditTextDialogClickListener okListener,
                                          final EditTextDialogClickListener cancelListener) {

        showEditTextDialog(activity, activity.getString(title), activity.getString(message), ok, cancel, okListener, cancelListener);
    }

    public static void showEditTextDialog(Activity activity, String title, String message, int ok, int cancel, final EditTextDialogClickListener okListener,

                                          final EditTextDialogClickListener cancelListener)

    {

        final AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);

        renameDialog.setTitle(title);
        renameDialog.setMessage(message);
        LinearLayout layout = new LinearLayout(activity);

        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final EditTextFont editTextNewFolder = new EditTextFont(activity);
        int p = (int) activity.getResources().getDimension(R.dimen.activity_horizontal_margin);
        layout.setPadding(p, p, p, p);
        layout.addView(editTextNewFolder, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        renameDialog.setView(layout);

        renameDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null)
                    okListener.onClick(dialog, whichButton, editTextNewFolder);
            }
        });

        renameDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (cancelListener != null)
                    cancelListener.onClick(dialog, whichButton, editTextNewFolder);
            }
        });
        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
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
        });
    }

    public static void showCustomDialog(Activity activity, int title, View layout, int ok, int cancel, final DialogInterface.OnClickListener okListener,
                                        final DialogInterface.OnClickListener cancelListener) {

        final AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);

        renameDialog.setTitle(title);
        renameDialog.setView(layout);

        renameDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null)
                    okListener.onClick(dialog, whichButton);
            }
        });

        renameDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (cancelListener != null)
                    cancelListener.onClick(dialog, whichButton);
            }
        });
        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
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
        });
    }

    public static void showCustomDialog(Activity activity, View layout, int ok, final DialogInterface.OnClickListener okListener) {

        final AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);

        //	renameDialog.setTitle(title);

        renameDialog.setView(layout);

        renameDialog.setPositiveButton(ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null)
                    okListener.onClick(dialog, whichButton);
            }
        });


        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog1) {

                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.message));
                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(android.R.id.title));

                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));

                    }
                });

                dialog.show();
            }
        });
    }

    /**
     * show dialog message. they try to get a readable message from exception
     *
     * @param activity
     * @param headline
     * @param message
     * @param e
     */
    public static void showDialogMessage(Activity activity, int headline, int message, Exception e) {

        String reason = getUserReadableMessage(activity, e);
        showDialogMessage(activity, headline, activity.getString(message) + (reason == null ? "" : reason));
    }

    private static String getUserReadableMessage(Activity activity, Exception e) {

        String reason = activity.getString(R.string.reason_for_error);
        if (e instanceof IOException) {
            return " " + reason.replace("%1", activity.getString(R.string.error_reason_io));
        }
        return null;
    }

    public static void hideKeyboard(Activity activity, View mView) {
        InputMethodManager imm = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);

    }

    public interface EditTextDialogClickListener {

        void onClick(DialogInterface dialog, int which, EditText editText);
    }
}
