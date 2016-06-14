package de.qabel.qabelbox;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.helper.AccountHelper;

public class QblJUnitRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }

    @Override
    public void onCreate(Bundle arguments) {
        CreateIdentityActivity.FAKE_COMMUNICATION = true;
        arguments.putString("disableAnalytics", "true");
        AccountHelper.SYNC_INTERVAL = 0;
        super.onCreate(arguments);
    }

}
