package de.qabel.qabelbox.chat;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
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

/**
 * Created by danny on 19.02.16.
 */
public class ShareHelper {

    private static String TAG = "ShareHelper";


    public static void shareToQabelUser(final LocalQabelService mService, final MainActivity context, final BoxNavigation nav, final Contact contact, final BoxFile boxFileOriginal) {

        {

            new AsyncTask<Void, String[], String[]>() {
                public AlertDialog waitMessage;

                void share(String url, String key) {

                    final ChatServer cs = ChatServer.getInstance(mService.getActiveIdentity());
                    final DropMessage dm = cs.getShareDropMessage( boxFileOriginal.name, url, key);
                    try {
                        mService.sendDropMessage(dm, contact, mService.getActiveIdentity(), new LocalQabelService.OnSendDropMessageResult() {
                            @Override
                            public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {

                                ChatMessageItem message = cs.createOwnMessage(mService.getActiveIdentity(), contact.getEcPublicKey().getReadableKeyIdentifier(), dm.getDropPayload(), dm.getDropPayloadType());
                                cs.storeIntoDB(message);
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText(context, R.string.messsage_file_shared, Toast.LENGTH_SHORT).show();
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
                    if (strings != null && strings.length == 2) {
                        share(strings[0], strings[1]);
                    }
                    waitMessage.dismiss();
                }

                @Override
                protected void onPreExecute() {

                    waitMessage = UIHelper.showWaitMessage(context, R.string.dialog_headline_info, R.string.dialog_share_sending_in_progress, false);
                }

                @Override
                protected String[] doInBackground(Void... params) {

                    try {

                        final QblECPublicKey OWNER = new QblECKeyPair().getPub();

                        BoxFile boxFile = nav.upload("foobar", new ByteArrayInputStream(new byte[100]), null);
                        nav.commit();

                        BoxExternalReference boxExternalReference = null;
                        try {
                            boxExternalReference = nav.createFileMetadata(OWNER, boxFile);
                            return new String[]{
                                    boxExternalReference.url, Hex.toHexString(boxExternalReference.key)
                            };
                        } catch (QblStorageException e) {
                            e.printStackTrace();
                        }
                    } catch (QblStorageException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
