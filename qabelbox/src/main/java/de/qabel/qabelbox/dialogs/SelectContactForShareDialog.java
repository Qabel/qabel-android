package de.qabel.qabelbox.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.qabel.core.config.Contact;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.helper.UIHelper;

import java.util.Iterator;
import java.util.Set;

public class SelectContactForShareDialog {
    public SelectContactForShareDialog(final MainActivity activity, final Result result) {
        LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layout.dialog_select_contact, null);
        final Spinner mIdentitySpinner = (Spinner) view.findViewById(id.spinner_identities);

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

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, layout.view_spinner, spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(layout.view_spinner);
        mIdentitySpinner.setAdapter(spinnerArrayAdapter);
        UIHelper.showCustomDialog(activity, string.headline_share_to_qabeluser, view, string.ok, string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Contact identity = contactList[mIdentitySpinner.getSelectedItemPosition()];
                        result.onContactSelected(identity);
                    }
                }
                , new OnClickListener() {
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
