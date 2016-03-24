package de.qabel.qabelbox;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

public class QblJUnitRunner extends AndroidJUnitRunner{

    @Override
    public void onCreate(Bundle arguments) {
        arguments.putString("disableAnalytics", "true");
		arguments.putString("notPackage", BuildConfig.APPLICATION_ID + ".ui");
		super.onCreate(arguments);
    }

}
