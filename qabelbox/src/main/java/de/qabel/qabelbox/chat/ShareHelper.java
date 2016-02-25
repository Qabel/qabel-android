package de.qabel.qabelbox.chat;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.spongycastle.util.encoders.Hex;

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

/**
 * Created by danny on 19.02.16.
 */
public class ShareHelper {

	private static final String TAG = "ShareHelper";

	/**
	 * convert chatmessageintem to boxexternalreferenz
	 *
	 * @param contact
	 * @param item
	 * @return
	 */
	@NonNull
	public static BoxExternalReference getBoxExternalReference(Contact contact, ChatMessageItem item) {

		ChatMessageItem.ShareMessagePayload payload = (ChatMessageItem.ShareMessagePayload) item.getData();
		return new BoxExternalReference(false, payload.getURL(), payload.getMessage(), contact.getEcPublicKey(), Hex.decode(payload.getKey()));
	}

	/**
	 * share file to contact.
	 * <p/>
	 * 1. Download boxoject
	 * 2. Upload with new key
	 * 3. BoxExternalReference
	 * 4. Send drop message
	 *
	 * @param mainActivity
	 * @param contact
	 * @param boxObject
	 */
	public static void shareToQabelUser(final MainActivity mainActivity, final Contact contact, final BoxFile boxObject) {

		final BoxNavigation nav = mainActivity.filesFragment.getBoxNavigation();
		final LocalQabelService mService = mainActivity.mService;
		final ChatServer cs = ChatServer.getInstance(mainActivity.mService.getActiveIdentity());

		new AsyncTask<Void, Integer, DropMessage>() {
			public AlertDialog waitMessage;

			@Override
			protected void onPreExecute() {
				//show wait message
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
								ChatMessageItem message = cs.createOwnMessage(mainActivity.mService.getActiveIdentity(), contact.getEcPublicKey().getReadableKeyIdentifier(), dm.getDropPayload(), dm.getDropPayloadType());
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
