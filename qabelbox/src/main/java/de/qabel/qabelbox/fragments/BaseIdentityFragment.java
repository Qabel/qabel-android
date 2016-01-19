package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;

import de.qabel.qabelbox.activities.CreateIdentityActivity;

/**
 * Created by danny on 19.01.16.
 */
public class BaseIdentityFragment extends Fragment {

    protected CreateIdentityActivity mActivty;

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        mActivty=(CreateIdentityActivity)activity;
    }

    /**
     * return true if all data entered
     *
     * @return
     */
    public String check() {

        return "";
    }

    /**
     * reset data for reuse fragment
     */
    public void resetData() {

    }

    public void onBackPressed() {

    }
}
