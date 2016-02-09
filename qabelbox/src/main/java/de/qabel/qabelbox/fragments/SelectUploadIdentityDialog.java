package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Iterator;
import java.util.Set;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * class to show identitiy list dialog for uploading
 * Created by danny on 09.02.2016.
 */
public class SelectUploadIdentityDialog {
    public SelectUploadIdentityDialog(final MainActivity activity, final Result result) {
        LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_select_identity, null);
        final Spinner mIdentitySpinner = (Spinner) view.findViewById(R.id.spinner_identities);
        final Set<Identity> identities = QabelBoxApplication.getInstance().getService().getIdentities().getIdentities();
        final String[] spinnerArray = new String[identities.size()];
        final Identity[] identityList = new Identity[identities.size()];
        Iterator<Identity> it = identities.iterator();
        int i = 0;
        while (it.hasNext()) {
            identityList[i] = it.next();
            spinnerArray[i] = identityList[i].getAlias();
            i++;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIdentitySpinner.setAdapter(spinnerArrayAdapter);
        UIHelper.showCustomeDialog(activity, R.string.headline_share_select_upload_identity, view, R.string.ok, R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Identity identity = identityList[mIdentitySpinner.getSelectedItemPosition()];
                        result.onIdentitySelected(identity);
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

        void onIdentitySelected(Identity identity);
    }
}
