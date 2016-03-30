package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;

public class ExternalApps {
    /**
     * start share dialog
     *
     * @param activity context
     * @param uri      uri to share
     * @param type     mimetype
     */
    public static void share(Activity activity, Uri uri, String type) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setData(uri);
        if (type != null) {
            shareIntent.setType(type);
        }
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, string.share_subject);
        shareIntent.putExtra(Intent.EXTRA_TITLE, string.share_subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(string.share_text));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(Intent.createChooser(shareIntent, activity.getString(string.share_via)), MainActivity.REQUEST_EXTERN_SHARE_APP);
    }

    /**
     * open extern app
     *
     * @param activity context
     * @param uri      uri to share
     * @param type     mimetype
     * @param action   send action, e.g. ACTION_VIEW, ACTION_EDIT
     */
    public static void openExternApp(Activity activity, Uri uri, String type, String action) {
        Intent viewIntent = new Intent();
        viewIntent.setDataAndType(uri, type);
        viewIntent.setAction(action);
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(Intent.createChooser(viewIntent, activity.getString(string.chooser_open_with)), MainActivity.REQUEST_EXTERN_VIEWER_APP);
    }
}
