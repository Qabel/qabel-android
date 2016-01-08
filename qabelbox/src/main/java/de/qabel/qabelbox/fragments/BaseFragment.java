package de.qabel.qabelbox.fragments;

import android.app.Fragment;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 08.01.2016.
 */
public class BaseFragment extends Fragment {
    protected static Executor serialExecutor = Executors.newSingleThreadExecutor();

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
}
