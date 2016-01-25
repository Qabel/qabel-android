package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;

import de.qabel.qabelbox.activities.BaseWizwardActivity;
import de.qabel.qabelbox.activities.CreateIdentityActivity;

/**
 * Created by danny on 19.01.16.
 */
public class BaseIdentityFragment extends Fragment {

    protected BaseWizwardActivity mActivty;

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        mActivty = (BaseWizwardActivity) activity;
    }

    /**
     * check if fragment ready to go to next page.
     *
     * @return error message to show to user or null if all ok
     */
    public String check() {

        return "";
    }
}
