package de.qabel.qabelbox.fragments;

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

    public void exportContact(Contact contact) {
        exportedContactCount = 1;
        String contactJson = ContactExportImport.exportContact(contact);
        startExportFileChooser(contact.getAlias(), QabelSchema.FILE_PREFIX_CONTACT, contactJson);

    }

    protected void handleScanResult(IntentResult scanResult) {
        String[] result = scanResult.getContents().split("\\r?\\n");
        if (result.length == 4 && result[0].equals("QABELCONTACT")) {
            try {
                DropURL dropURL = new DropURL(result[2]);
                Collection<DropURL> dropURLs = new ArrayList<>();
                dropURLs.add(dropURL);

                QblECPublicKey publicKey = new QblECPublicKey(Hex.decode(result[3]));
                Contact contact = new Contact(result[1], dropURLs, publicKey);
                addContactSilent(contact);
            } catch (Exception e) {
                Log.w(TAG, "add contact failed", e);
                UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, e);
            }
        }
    }

    public void exportAllContacts() {
        try {
            LocalQabelService service = QabelBoxApplication.getInstance().getService();
            Contacts contacts = service.getContacts(service.getActiveIdentity());
            exportedContactCount = contacts.getContacts().size();
            if (exportedContactCount > 0) {
                String contactJson = ContactExportImport.exportContacts(contacts);
                startExportFileChooser("", QabelSchema.FILE_PREFIX_CONTACTS, contactJson);
            }

        } catch (JSONException e) {
            Log.e(TAG, "error on export contacts", e);
            UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_warning, R.string.cant_export_contacts);
        }
    }

    public void enableDocumentProvider(boolean value) {
        useDocumentProvider = value;
    }

    /**
     * add contact and show messages
     *
     * @param contact
     */
    protected void addContactSilent(Contact contact) throws QblStorageEntityExistsException {
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        service.addContact(contact);
        sendRefreshContactList();
    }

    protected void sendRefreshContactList() {
        Log.d(TAG, "send refresh intent");
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        QabelBoxApplication.getInstance().getApplicationContext().sendBroadcast(intent);
    }


    public void importContactFromUri(MainActivity mActivity, Uri uri) {


        Log.d(TAG, "import contact from uri " + uri);
        try {
            int added = 0;
            ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "r");
            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            String json = FileHelper.readFileAsText(fis);
            fis.close();
            ContactExportImport.ContactsParseResult result = ContactExportImport.parse(QabelBoxApplication.getInstance().getService().getActiveIdentity(), json);
            int failed = result.getSkippedContacts();
            Contacts contacts = result.getContacts();
            for (Contact contact : contacts.getContacts()) {
                try {
                    addContactSilent(contact);
                    added++;
                } catch (QblStorageEntityExistsException existsException) {
                    Log.w(TAG, "found doublet's. Will ignore it", existsException);
                    failed++;
                }
            }
            if (added == 1 && failed == 0) {
                UIHelper.showDialogMessage(
                        mActivity,
                        mActivity.getString(R.string.dialog_headline_info),
                        mActivity.getResources().getString(R.string.contact_import_successfull, added, (added + failed))
                );
            } else {
                UIHelper.showDialogMessage(
                        mActivity,
                        mActivity.getString(R.string.dialog_headline_info),
                        mActivity.getResources().getString(R.string.contact_import_successfull_many, added, (added + failed))
                );
            }

        } catch (IOException | JSONException ioException) {
            UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, ioException);
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
