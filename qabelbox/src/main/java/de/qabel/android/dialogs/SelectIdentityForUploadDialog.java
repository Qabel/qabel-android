package de.qabel.android.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Iterator;
import java.util.Set;

import de.qabel.core.config.Identity;
import de.qabel.android.R;
import de.qabel.android.activities.MainActivity;
import de.qabel.android.helper.UIHelper;

/**
 * class to show identitiy list dialog for uploading
 * Created by danny on 09.02.2016.
 */
public class SelectIdentityForUploadDialog {

    public SelectIdentityForUploadDialog(final MainActivity activity, final Result result) {

        LayoutInflater inflater = (LayoutInflater)
                activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_select_identity, null);
        final Spinner mIdentitySpinner = (Spinner) view.findViewById(R.id.spinner_identities);

        final Set<Identity> identities = activity.mService.getIdentities().getIdentities();
        final String[] spinnerArray = new String[identities.size()];
        final Identity[] identityList = new Identity[identities.size()];
        Iterator<Identity> it = identities.iterator();
        int i = 0;
        while (it.hasNext()) {
            identityList[i] = it.next();
            spinnerArray[i] = identityList[i].getAlias();
            i++;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, R.layout.view_spinner, spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.view_spinner);
        mIdentitySpinner.setAdapter(spinnerArrayAdapter);
        UIHelper.showCustomDialog(activity, R.string.headline_share_into_app, view, R.string.ok, R.string.cancel, new DialogInterface.OnClickListener() {
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
