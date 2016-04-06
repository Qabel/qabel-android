package de.qabel.qabelbox.fragments;

import android.content.Intent;
import android.util.Log;

import de.qabel.core.config.Contact;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Created by danny on 29.03.16.
 */
public class ContactBaseFragment extends BaseFragment {
    private final String TAG = this.getClass().getSimpleName();







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





}
