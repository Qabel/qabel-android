package de.qabel.qabelbox.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Iterator;
import java.util.Set;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * class to show identitiy list dialog for uploading
 * Created by danny on 09.02.2016.
 */
public class SelectContactForShareDialog {

    public SelectContactForShareDialog(final MainActivity activity, final Result result) {

        LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_select_contact, null);
        final Spinner mIdentitySpinner = (Spinner) view.findViewById(R.id.spinner_identities);

        Set<Contact> identities = activity.mService.getContacts().getContacts();
        final String[] spinnerArray = new String[identities.size()];
        final Contact[] contactList = new Contact[identities.size()];
        Iterator<Contact> it = identities.iterator();
        int i = 0;
        while (it.hasNext()) {
            contactList[i] = it.next();
            spinnerArray[i] = contactList[i].getAlias();
            i++;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, R.layout.view_spinner, spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.view_spinner);
        mIdentitySpinner.setAdapter(spinnerArrayAdapter);
        UIHelper.showCustomeDialog(activity, R.string.headline_share_to_qabeluser, view, R.string.ok, R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Contact identity = contactList[mIdentitySpinner.getSelectedItemPosition()];
                        result.onContactSelected(identity);
                    }
                }
                , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        result.onCancel();
                    }
                }
        );
    }

    public interface Result {

        void onCancel();

        void onContactSelected(Contact contact);
    }
}
