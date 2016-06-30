package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.content.Intent;

import de.qabel.core.config.Identities;
import de.qabel.qabelbox.activities.BaseWizardActivity;
import de.qabel.qabelbox.activities.CreateAccountActivity;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.config.AppPreference;

public class Sanity {

    /**
     * start wizard activities if app not ready to go. If other activity need to start, the current activity finished
     *
     * @param activity current activity
     * @param identities
     * @return false if no wizard start needed
     */
    public static boolean startWizardActivities(Activity activity, Identities identities) {

        AppPreference prefs = new AppPreference(activity);

        if (prefs.getToken() == null || prefs.getAccountName() == null) {
            Intent intent = new Intent(activity, CreateAccountActivity.class);
            intent.putExtra(BaseWizardActivity.FIRST_RUN, true);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            activity.finish();
            return true;
        } else {
            if (identities.getIdentities().size() == 0) {
                Intent intent = new Intent(activity, CreateIdentityActivity.class);
                intent.putExtra(BaseWizardActivity.FIRST_RUN, true);
                activity.startActivity(intent);
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                activity.finish();
                return true;
            } else {
                return false;
            }
        }
    }
}
