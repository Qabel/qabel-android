package de.qabel.android.helper;

import android.app.Activity;
import android.content.Intent;

import de.qabel.android.QabelBoxApplication;
import de.qabel.android.activities.BaseWizardActivity;
import de.qabel.android.activities.CreateAccountActivity;
import de.qabel.android.activities.CreateIdentityActivity;
import de.qabel.android.config.AppPreference;
import de.qabel.android.services.LocalQabelService;

/**
 * Created by danny on 10.02.16.
 */
public class Sanity {

    /**
     * start wizard activities if app not ready to go. If other activity need to start, the current activity finished
     *
     * @param activity current activity
     * @return false if no wizard start needed
     */
    public static boolean startWizardActivities(Activity activity) {

        AppPreference prefs = new AppPreference(activity);

        if (prefs.getToken() == null) {
            Intent intent = new Intent(activity, CreateAccountActivity.class);
            intent.putExtra(BaseWizardActivity.FIRST_RUN, true);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            activity.finish();
            return true;
        } else {
            LocalQabelService service = QabelBoxApplication.getInstance().getService();
            if (service != null && service.getIdentities().getIdentities().size() == 0) {
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
