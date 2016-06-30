package de.qabel.qabelbox.chat;

import android.app.Activity;
import android.content.Intent;

import de.qabel.qabelbox.R;

public class ShareHelper {

    private static final String TAG = "ShareHelper";

    public static void tellAFriend(Activity activity) {

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.tellAFriendSubject));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.tellAFriendMessage));
        activity.startActivity(Intent.createChooser(sharingIntent, activity.getResources().getText(R.string.share_via)));
    }
}
