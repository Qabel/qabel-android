package de.qabel.qabelbox.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.CrashReportingActivity;
import de.qabel.qabelbox.dagger.HasComponent;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.settings.dagger.SettingsActivityComponent;
import de.qabel.qabelbox.settings.dagger.SettingsActivityModule;
import de.qabel.qabelbox.settings.navigation.SettingsNavigator;

public class SettingsActivity extends CrashReportingActivity
        implements HasComponent<SettingsActivityComponent>{

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    SettingsNavigator navigator;

    private SettingsActivityComponent activityComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent = getApplicationComponent().
                plus(new ActivityModule(this)).
                plus(new SettingsActivityModule(this));

        activityComponent.inject(this);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setupToolbar();

        navigator.selectSettingsFragment();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert getSupportActionBar() != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.headline_settings);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }


    @Override
    public SettingsActivityComponent getComponent() {
        return activityComponent;
    }
}
