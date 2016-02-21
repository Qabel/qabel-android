package de.qabel.qabelbox.chat;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.spongycastle.util.encoders.Hex;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalReference;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;

/**
 * Created by danny on 19.02.16.
 */
public class ShareHelper {

    private static String TAG = "ShareHelper";

    @NonNull
    public static BoxExternalReference getBoxExternalReference(Contact contact, ChatMessageItem item) {

        ChatMessageItem.ShareMessagePayload payload = (ChatMessageItem.ShareMessagePayload) item.getData();
        return new BoxExternalReference(false, payload.getURL(), payload.getMessage(), contact.getEcPublicKey(), Hex.decode(payload.getKey()));
    }

    public static void shareToQabelUser(final MainActivity mainActivity, final Contact contact, final Uri fileUri, final BoxObject boxFileOriginal) {

        final BoxNavigation nav = mainActivity.filesFragment.getBoxNavigation();
        final LocalQabelService mService = mainActivity.mService;
        {
            new AsyncTask<Void, String[], String[]>() {
                public AlertDialog waitMessage;

                private void share(String url, String key, String name) {

                    final ChatServer cs = ChatServer.getInstance(mainActivity.mService.getActiveIdentity());
                    final DropMessage dm = cs.getShareDropMessage(name, url, key);
                    try {
                        mService.sendDropMessage(dm, contact, mService.getActiveIdentity(), new LocalQabelService.OnSendDropMessageResult() {
                            @Override
                            public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {

                                ChatMessageItem message = cs.createOwnMessage(mainActivity.mService.getActiveIdentity(), contact.getEcPublicKey().getReadableKeyIdentifier(), dm.getDropPayload(), dm.getDropPayloadType());
                                cs.storeIntoDB(message);
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText(mainActivity, R.string.messsage_file_shared, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    } catch (
                            QblDropPayloadSizeException e
                            ) {
                        Log.e(TAG, "cant send share", e);
                    }
                }

                @Override
                protected void onPostExecute(String[] strings) {

                    super.onPostExecute(strings);
                    if (strings != null && strings.length == 3) {
                        share(strings[0], strings[1], strings[2]);
                    }
                    waitMessage.dismiss();
                }

                @Override
                protected void onPreExecute() {

                    waitMessage = UIHelper.showWaitMessage(mainActivity, R.string.dialog_headline_info, R.string.dialog_share_sending_in_progress, false);
                }

                @Override
                protected String[] doInBackground(Void... params) {

                    try {
                        InputStream content = mainActivity.getContentResolver().openInputStream(fileUri);
                        BoxFile boxFile = nav.upload(contact.getAlias() + "-" + boxFileOriginal.name, content, null);
                        nav.commit();
                        //@todo if this correct?
                        nav.detachExternal(boxFile.name);
                        BoxExternalReference boxExternalReference = null;
                        try {
                            boxExternalReference = nav.createFileMetadata(mService.getActiveIdentity().getEcPublicKey(), boxFile);
                            return new String[]{
                                    /*boxExternalReference.getPrefix()+boxExternalReference.getBlock()*/
                                    boxExternalReference.url, Hex.toHexString(boxExternalReference.key), boxExternalReference.name
                            };
                        } catch (QblStorageException e) {
                            e.printStackTrace();
                        }
                    } catch (QblStorageException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
