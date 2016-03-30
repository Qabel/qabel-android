package de.qabel.qabelbox.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import de.qabel.core.config.Contact;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.dialogs.SelectContactForShareDialog;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalReference;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import org.spongycastle.util.encoders.Hex;

import java.util.Map;

/**
 * Created by danny on 19.02.16.
 */
public class ShareHelper {

    private static final String TAG = "ShareHelper";

    public static void tellAFriend(Activity activity) {

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.tellAFriendSubject));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.tellAFriendMessage));
        activity.startActivity(Intent.createChooser(sharingIntent, activity.getResources().getText(R.string.share_via)));
    }

    public static void shareToQabelUser(final MainActivity self, LocalQabelService mService, final BoxObject boxObject) {

        if (mService.getContacts(mService.getActiveIdentity()).getContacts().size() == 0) {
            UIHelper.showDialogMessage(self, R.string.dialog_headline_info, R.string.cant_share_contactlist_is_empty);

        } else {
            if (boxObject instanceof BoxFile) {

                new SelectContactForShareDialog(self, new SelectContactForShareDialog.Result() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onContactSelected(Contact contact) {

                        shareToQabelUser(self, contact, (BoxFile) boxObject);
                    }
                });
            } else {
                UIHelper.showDialogMessage(self, R.string.share_only_files_possibility, Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * convert chatmessageintem to boxexternalreferenz
     */
    @NonNull
    public static BoxExternalReference getBoxExternalReference(Contact contact, ChatMessageItem item) {

        ChatMessageItem.ShareMessagePayload payload = (ChatMessageItem.ShareMessagePayload) item.getData();
        return new BoxExternalReference(false, payload.getURL(), null, contact.getEcPublicKey(), Hex.decode(payload.getKey()));
    }

    /**
     * share file to contact.
     * <p/>
     * 1. Download boxoject
     * 2. Upload with new key
     * 3. BoxExternalReference
     * 4. Send drop message
     */
    private static void shareToQabelUser(final MainActivity mainActivity, final Contact contact, final BoxFile boxObject) {

        final BoxNavigation nav = mainActivity.filesFragment.getBoxNavigation();
        final LocalQabelService mService = mainActivity.mService;
        final ChatServer cs = mainActivity.chatServer;

        new AsyncTask<Void, Integer, DropMessage>() {
            public AlertDialog waitMessage;

            @Override
            protected void onPreExecute() {
                waitMessage = UIHelper.showWaitMessage(mainActivity, R.string.dialog_headline_info, R.string.dialog_share_sending_in_progress, false);
            }

            @Override
            protected DropMessage doInBackground(Void... params) {

                try {
                    BoxExternalReference boxExternalReference = nav.createFileMetadata(mService.getActiveIdentity().getEcPublicKey(), boxObject);
                    nav.commit();
                    return cs.getShareDropMessage(boxExternalReference.name, boxExternalReference.url, Hex.toHexString(boxExternalReference.key));
                } catch (QblStorageException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(final DropMessage dm) {

                if (dm != null) {
                    try {
                        mService.sendDropMessage(dm, contact, mService.getActiveIdentity(), new LocalQabelService.OnSendDropMessageResult() {
                            @Override
                            public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {
                                ChatMessageItem message = new ChatMessageItem(mainActivity.mService.getActiveIdentity(), contact.getEcPublicKey().getReadableKeyIdentifier(), dm.getDropPayload(), dm.getDropPayloadType());
                                cs.storeIntoDB(message);
                                mainActivity.filesFragment.refresh();
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mainActivity, R.string.messsage_file_shared, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        });
                    } catch (QblDropPayloadSizeException e) {
                        Log.e(TAG, "cant send share", e);
                        UIHelper.showDialogMessage(mainActivity, R.string.dialog_headline_warning, R.string.share_error_on_sending, e);
                    }
                } else {
                    UIHelper.showDialogMessage(mainActivity, R.string.dialog_headline_warning, R.string.share_error_on_sending);
                }
                //hide wait message
                waitMessage.dismiss();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
