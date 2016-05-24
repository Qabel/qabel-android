package de.qabel.qabelbox.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.dagger.components.ActivityComponent;
import de.qabel.qabelbox.dagger.components.ApplicationComponent;
import de.qabel.qabelbox.dagger.modules.ActivityModule;

public abstract class BaseActivity extends AppCompatActivity {


    private ActivityComponent activityComponent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent = getApplicationComponent()
                .plus(new ActivityModule(this));
    }

    public ActivityComponent getComponent() {
        return activityComponent;
    }


    private ApplicationComponent getApplicationComponent() {
        return QabelBoxApplication.getApplicationComponent(getApplicationContext());
    }

}
