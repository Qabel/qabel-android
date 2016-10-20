package de.qabel.qabelbox.startup.fragments;

import android.app.Activity;
import android.app.Fragment;

import de.qabel.qabelbox.startup.activities.BaseWizardActivity;

public class BaseIdentityFragment extends Fragment {

    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String ACCOUNT_EMAIL = "ACCOUNT_EMAIL";
    protected BaseWizardActivity mActivity;

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        mActivity = (BaseWizardActivity) activity;
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
