package de.qabel.qabelbox.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.View;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.dagger.HasComponent;
import de.qabel.qabelbox.listeners.IdleCallback;

/**
 * Base Fragment
 * Created by danny on 08.01.2016.
 */
public abstract class BaseFragment extends Fragment {

    protected static Executor serialExecutor = Executors.newSingleThreadExecutor();
    protected ActionBar actionBar;

    protected MainActivity mActivity;

    @Nullable
    protected IdleCallback idle;

    public void setIdleCallback(@Nullable IdleCallback idle) {
        this.idle = idle;
    }

    public void busy() {
        if (idle != null) {
            idle.busy();
        }
    }

    public void idle() {
        if (idle != null) {
            idle.idle();
        }
    }

    @SuppressWarnings("unchecked")
    protected <C> C getComponent(Class<C> componentType) {
        return componentType.cast(((HasComponent<C>)getActivity()).getComponent());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        actionBar = mActivity.getSupportActionBar();
    }

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
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        actionBar = null;
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

        mActivity.toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mActivity.onBackPressed();
            }
        });
    }

    /**
     * set own back listener in actionbar
     */
    protected void setActionBarBackListener(final View.OnClickListener listener) {

        mActivity.toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
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
