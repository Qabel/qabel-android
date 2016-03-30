package de.qabel.qabelbox.activities;

import android.R.anim;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.fragments.SettingsFragment;

public class SettingsActivity extends CrashReportingActivity {
    private SettingsActivity mActivity;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        overridePendingTransition(anim.fade_in, anim.fade_out);
        setContentView(layout.activity_settings);
        setupToolbar();
        createFragments();
        actionBar.setTitle(string.headline_settings);
    }

    private void setupToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(id.toolbar);
        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();
        assert getSupportActionBar() != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    private void createFragments() {
        SettingsFragment fragment = new SettingsFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(id.fragment_container_content, fragment);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }


}
