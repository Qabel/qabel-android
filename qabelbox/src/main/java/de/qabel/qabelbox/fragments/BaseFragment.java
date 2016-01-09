package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

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
    protected ActionBar action;
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
        action = mActivity.getSupportActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        action.setTitle(getTitle());
    }

    public void showUpButton() {
        //final View.OnClickListener clickListener = mActivity.toggle.getToolbarNavigationClickListener();

        mActivity.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getFragmentManager().popBackStack();


            }
        });
    }


}
