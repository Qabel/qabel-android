package de.qabel.qabelbox;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.providers.BoxProvider;

public class QblJUnitRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {
        BoxProvider.defaultTransferManager = "fake";
        CreateIdentityActivity.FAKE_COMMUNICATION = true;
        arguments.putString("disableAnalytics", "true");
        AccountHelper.SYNC_INTERVAL = 0;
        super.onCreate(arguments);
    }

}
