package de.qabel.qabelbox.helper;

import android.R.id;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.dimen;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.R.style;
import de.qabel.qabelbox.views.EditTextFont;

import java.io.IOException;

/**
 * Class to support app wide helper function
 * Created by danny on 10.01.2016.
 *
 * @deprecated: This should be moved to an upper BaseFragment or BaseActivity class
 */
@Deprecated
public class UIHelper {

    /**
     * show dialog with one buttonhelp
     */
    public static void showDialogMessage(final Activity activity, final int headline, final int message, final int buttonOk, final int buttonCancel, final OnClickListener buttonOkListener, final OnClickListener buttonCancelListener) {

        showDialogMessage(activity, activity.getString(headline), activity.getString(message), buttonOk, buttonCancel, buttonOkListener, buttonCancelListener);
    }

    /**
     * show dialog with one button
     */
    public static void showDialogMessage(final Activity activity, final String headline, final String message, final int buttonOk, final int buttonCancel, final OnClickListener buttonOkListener, final OnClickListener buttonCancelListener) {

        final Builder builder =
                new Builder(activity);
        builder.setTitle(headline);
        builder.setMessage(message);
        builder.setPositiveButton(buttonOk, buttonOkListener);
        if (buttonCancel != Integer.MIN_VALUE) {
            builder.setNegativeButton(buttonCancel, buttonCancelListener);
        }

        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog1) {

                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.message));
                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.title));

                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
                    }
                });

                dialog.show();
            }
        });
    }

    public static void showDialogMessage(Activity activity, int headline, int message) {

        showDialogMessage(activity, headline, message, string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, int headline, String message) {

        showDialogMessage(activity, activity.getString(headline), message, string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, String headline, String message) {
        showDialogMessage(activity, headline, message, string.ok, Integer.MIN_VALUE, null, null);
    }

    public static void showDialogMessage(Activity activity, int headline, int message, OnClickListener buttonOkListener) {

        showDialogMessage(activity, headline, message, string.ok, Integer.MIN_VALUE, buttonOkListener, null);
    }

    public static void showDialogMessage(Activity activity, int headline, int message, int buttonOk) {

        showDialogMessage(activity, headline, message, buttonOk, Integer.MIN_VALUE, null, null);
    }

    public static void showFunctionNotYetImplemented(Activity activity) {

        showDialogMessage(activity, string.dialog_headline_info, string.function_not_yet_implenented);
    }

    /**
     * show wait message
     */
    public static AlertDialog showWaitMessage(final Activity activity, int headline, int message, boolean cancelable) {

        Builder builder =
                new Builder(activity, style.AppCompatAlertDialogStyle);
        builder.setTitle(headline);
        builder.setMessage(message);

        builder.setCancelable(cancelable);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog1) {

                final FontHelper fontHelper = FontHelper.getInstance();
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.message));
                fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.title));
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

        final Builder renameDialog = new Builder(activity);

        renameDialog.setTitle(title);
        renameDialog.setMessage(message);
        LinearLayout layout = new LinearLayout(activity);

        layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        final EditTextFont editTextNewFolder = new EditTextFont(activity);
        int p = (int) activity.getResources().getDimension(dimen.activity_horizontal_margin);
        layout.setPadding(p, p, p, p);
        layout.addView(editTextNewFolder, new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        renameDialog.setView(layout);

        renameDialog.setPositiveButton(string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null) {
                    okListener.onClick(dialog, whichButton, editTextNewFolder);
                }
            }
        });

        renameDialog.setNegativeButton(string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                if (cancelListener != null) {
                    cancelListener.onClick(dialog, whichButton, editTextNewFolder);
                }
            }
        });
        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
                dialog.setOnShowListener(new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog1) {

                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.message));
                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.title));

                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
                    }
                });

                dialog.show();
            }
        });
    }

    public static void showCustomDialog(Activity activity, int title, View layout, int ok, int cancel, final OnClickListener okListener,
                                        final OnClickListener cancelListener) {

        final Builder renameDialog = new Builder(activity);

        renameDialog.setTitle(title);
        renameDialog.setView(layout);

        renameDialog.setPositiveButton(string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null) {
                    okListener.onClick(dialog, whichButton);
                }
            }
        });

        renameDialog.setNegativeButton(string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                if (cancelListener != null) {
                    cancelListener.onClick(dialog, whichButton);
                }
            }
        });
        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
                dialog.setOnShowListener(new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog1) {

                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.message));
                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.title));

                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
                    }
                });

                dialog.show();
            }
        });
    }

    public static void showCustomDialog(Activity activity, View layout, int ok, final OnClickListener okListener) {

        final Builder renameDialog = new Builder(activity);

        //	renameDialog.setTitle(title);

        renameDialog.setView(layout);

        renameDialog.setPositiveButton(ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                if (okListener != null) {
                    okListener.onClick(dialog, whichButton);
                }
            }
        });


        final FontHelper fontHelper = FontHelper.getInstance();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final AlertDialog dialog = renameDialog.create();
                dialog.setOnShowListener(new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog1) {

                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.message));
                        fontHelper.setCustomeFonts((TextView) dialog.findViewById(id.title));

                        fontHelper.setCustomeFonts(dialog.getButton(AlertDialog.BUTTON_POSITIVE));

                    }
                });

                dialog.show();
            }
        });
    }

    /**
     * show dialog message. they try to get a readable message from exception
     */
    public static void showDialogMessage(Activity activity, int headline, int message, Exception e) {

        String reason = getUserReadableMessage(activity, e);
        showDialogMessage(activity, headline, activity.getString(message) + (reason == null ? "" : reason));
    }

    private static String getUserReadableMessage(Activity activity, Exception e) {

        String reason = activity.getString(string.reason_for_error);
        if (e instanceof IOException) {
            return " " + reason.replace("%1", activity.getString(string.error_reason_io));
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
