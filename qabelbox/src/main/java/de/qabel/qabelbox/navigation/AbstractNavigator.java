package de.qabel.qabelbox.navigation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.util.Log;

import de.qabel.qabelbox.R;

public class AbstractNavigator {

    protected void showFragment(Activity activity, Fragment fragment, String tag, boolean addToBackStack, boolean waitForTransaction) {
        FragmentTransaction fragmentTransaction = activity.getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag);
        if(addToBackStack){
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.commit();
        if (waitForTransaction) {
            try {
                while (activity.getFragmentManager().executePendingTransactions()) {
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Log.e(activity.getClass().getSimpleName(), "Error waiting for fragment change", e);
            }
        }
    }
}
