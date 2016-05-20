package de.qabel.qabelbox.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Created by danny on 29.03.16.
 */
public class ContactBaseFragment extends BaseFragment {
    public static final int REQUEST_IMPORT_CONTACT = 1000;
    public static final int REQUEST_EXPORT_CONTACT = 1001;

    private final String TAG = this.getClass().getSimpleName();
    protected String dataToExport;
    private boolean useDocumentProvider = true;//used for tests
    protected int exportedContactCount;

    /**
     * add contact and show messages
     *
     * @param contact
     */
    protected void addContactSilent(Context context, Contact contact) throws QblStorageEntityExistsException {
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        service.addContact(contact);
        sendRefreshContactList(context);
    }

    protected void sendRefreshContactList(Context context) {
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent);
    }


    public void importContactFromUri(MainActivity activity, Uri uri) {


        Log.d(TAG, "import contact from uri " + uri);
        try {
            int added = 0;
            ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "r");
            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            String json = FileHelper.readFileAsText(fis);
            fis.close();
            ContactExportImport.ContactsParseResult result = ContactExportImport
                    .parse(activity.getActiveIdentity(), json);
            int failed = result.getSkippedContacts();
            Contacts contacts = result.getContacts();
            for (Contact contact : contacts.getContacts()) {
                try {
                    addContactSilent(activity.getApplicationContext(), contact);
                    added++;
                } catch (QblStorageEntityExistsException existsException) {
                    Log.w(TAG, "found doublet's. Will ignore it", existsException);
                    failed++;
                }
            }
            if (added == 1 && failed == 0) {
                UIHelper.showDialogMessage(
                        activity,
                        activity.getString(R.string.dialog_headline_info),
                        activity.getResources().getString(R.string.contact_import_successfull)
                );
            } else {
                UIHelper.showDialogMessage(
                        activity,
                        activity.getString(R.string.dialog_headline_info),
                        activity.getResources().getString(R.string.contact_import_successfull_many, added, (added + failed))
                );
            }

        } catch (IOException | JSONException ioException) {
            UIHelper.showDialogMessage(activity, R.string.dialog_headline_warning, R.string.contact_import_failed, ioException);
        }
    }

    private void startExportFileChooser(String filename, String type, String data) {
        dataToExport = data;
        if (useDocumentProvider) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, type + "" + filename + "." + QabelSchema.FILE_SUFFIX_CONTACT);
            startActivityForResult(intent, REQUEST_EXPORT_CONTACT);
        }
    }
}
