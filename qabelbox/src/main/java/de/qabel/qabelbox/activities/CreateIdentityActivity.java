package de.qabel.qabelbox.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.CreateIdentityDescriptionFragment;
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityHeaderFragment;
import de.qabel.qabelbox.fragments.CreateIdentityNameFragment;
import de.qabel.qabelbox.fragments.CreateIdentitySecurityFragment;
import de.qabel.qabelbox.fragments.IdentityBaseFragment;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 11.01.2016.
 */
public class CreateIdentityActivity extends AppCompatActivity {

    private CreateIdentityActivity mActivity;
    private Toolbar mToolbar;
    MenuItem mActionNext;
    private ActionBar actionBar;
    private CreateIdentityHeaderFragment mIdentityHeaderFragment;
    private CreateIdentityNameFragment mIdentityNameFragment;
    int progress = 0;
    String name, description;
    int security = Integer.MIN_VALUE;
    private CreateIdentityDescriptionFragment mIdentityDecriptionFragment;
    private CreateIdentitySecurityFragment mIdentitySecurityFragment;
    private CreateIdentityFinalFragment mIdentityFinalFragment;
    IdentityBaseFragment[] fragments = new IdentityBaseFragment[4];
    int step = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_create_identity);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.headline_add_identity);
        actionBar.setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });
        mIdentityHeaderFragment = new CreateIdentityHeaderFragment();

        step = 0;
        fragments[0] = new CreateIdentityNameFragment();
        fragments[1] = new CreateIdentityDescriptionFragment();
        fragments[2] = new CreateIdentitySecurityFragment();
        fragments[3] = new CreateIdentityFinalFragment();
        getFragmentManager().beginTransaction().add(R.id.fragment_container_content, fragments[0]).addToBackStack(null).add(R.id.fragment_container_header, mIdentityHeaderFragment).commit();
    }

    @Override
    public void onBackPressed() {

        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
            step--;
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.ab_create_identity, menu);
        mActionNext = menu.findItem(R.id.action_next);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_next) {
            handleNextClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleNextClick() {

        if (name == null) {
            name = "123 john";
        } else if (description == null) {
            description = "description";
        } else if (security == Integer.MIN_VALUE) {
            security = 3;
        }
        if (((IdentityBaseFragment) getFragmentManager().findFragmentById(R.id.fragment_container_content)).check()) {
            step++;
            if (step == fragments.length) {
                completed();
                return;
            }
            mIdentityHeaderFragment.updateUI(name, description, security);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null).commit();
        }
        else
        {
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info, R.string.create_identity_enter_all_data,R.string.ok);
        }
    }

    private void completed() {

        setResult(RESULT_OK);
        finish();
    }
}
