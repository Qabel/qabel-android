package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Base Fragment
 * Created by danny on 08.01.2016.
 */
public abstract class BaseFragment extends Fragment {

    protected static Executor serialExecutor = Executors.newSingleThreadExecutor();
    protected ActionBar actionBar;
    protected MainActivity mActivity;

    /**
     * @return title for fragment
     */
    public String getTitle() {
        return getString(string.app_name);
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

    @Override
    public void onResume() {

        super.onResume();
        if (actionBar != null) {
            actionBar.setTitle(getTitle());
        }
        if (isFabNeeded()) {
            mActivity.fab.show();
        } else {
            mActivity.fab.hide();
        }
    }

    /**
     * set own back listener in actionbar
     */
    protected void setActionBarBackListener() {

        mActivity.toggle.setToolbarNavigationClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mActivity.onBackPressed();
            }
        });
    }

    /**
     * set own back listener in actionbar
     */
    protected void setActionBarBackListener(final OnClickListener listener) {

        mActivity.toggle.setToolbarNavigationClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (listener != null) {
                    listener.onClick(v);
                }
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

    /**
     * @return true if fragment handle back button. otherwise return false to display sideMenu icon
     */
    public boolean supportSubtitle() {

        return false;
    }

    /**
     * handle hardware back button
     */
    public void onBackPressed() {

    }

    public void updateSubtitle() {

    }

    public boolean handleFABAction() {

        return false;
    }
}
