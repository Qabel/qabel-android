package de.qabel.qabelbox.activities;

import android.support.v7.app.AppCompatActivity;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.dagger.components.ApplicationComponent;

public abstract class BaseActivity extends AppCompatActivity {


    public ApplicationComponent getApplicationComponent() {
        return QabelBoxApplication.getApplicationComponent(getApplicationContext());
    }

}
