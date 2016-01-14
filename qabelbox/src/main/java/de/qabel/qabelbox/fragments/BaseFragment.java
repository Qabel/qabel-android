package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.View;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;

/**
 * Base Fragment
 * Created by danny on 08.01.2016.
 */
public class BaseFragment extends Fragment {
    protected static Executor serialExecutor = Executors.newSingleThreadExecutor();
    protected ActionBar actionBar;
    protected MainActivity mActivity;

    /**
     * @return title for fragment
     */
    public String getTitle() {
        return getString(R.string.app_name);
    }

    /**
     * @return true if floating action button used
     */
    public boolean isFabNeeded() {
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) getActivity();
        actionBar = mActivity.getSupportActionBar();
    }

    /**
     * set own back listener in actionbar
     */
    protected void setActionBarBackListener() {
        mActivity.toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                if (/*fm != null &&*/ fm.getBackStackEntryCount() > 0)
                    mActivity.onBackPressed();
            }
        });
    }

    /**
     * @return true if fragment handle back button. otherwise return false to display sideMenu icon
     */
    public boolean supportBackButton() {
        return false;
    }
}
