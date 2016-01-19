package de.qabel.qabelbox.activities;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateIdentityDescriptionFragment;
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityHeaderFragment;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import de.qabel.qabelbox.fragments.CreateIdentityNameFragment;
import de.qabel.qabelbox.fragments.CreateIdentitySecurityFragment;
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

    BaseIdentityFragment[] fragments = new BaseIdentityFragment[5];
    int step = 0;
    private String TAG = this.getClass().getSimpleName();

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
        fragments[0] = new CreateIdentityMainFragment();
        fragments[1] = new CreateIdentityNameFragment();
        fragments[2] = new CreateIdentityDescriptionFragment();
        fragments[3] = new CreateIdentitySecurityFragment();
        fragments[4] = new CreateIdentityFinalFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container_content, fragments[0]);//.addToBackStack(null);
        ft.add(R.id.fragment_container_header, mIdentityHeaderFragment);
        ft.commit();
    }

    @Override
    public void onBackPressed() {

        System.out.println("onback " + getFragmentManager().getBackStackEntryCount());
        int fragmentCount = getFragmentManager().getBackStackEntryCount();
        if (step < fragments.length && fragmentCount > 0 ) {
            if(fragmentCount== fragments.length - 1)
            {
                getToMainFragment();
                return;
            }
            ((BaseIdentityFragment)getFragmentManager().findFragmentById(R.id.fragment_container_content)).onBackPressed();
            getFragmentManager().popBackStack();
            step--;
            mIdentityHeaderFragment.updateUI(name, description, security);
            return;
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void getToMainFragment() {

        Log.v(TAG, "get to main fragment");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            fragments[i].resetData();
        }
        int fragmentCount = getFragmentManager().getBackStackEntryCount();
        while (fragmentCount-- > 0) {
            getFragmentManager().popBackStack();
        }
        step = 0;
        //    ft.replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null);
        ft.commit();
        resetUI();

        updateActionBar(step);
    }

    private void resetUI() {

        setIdentityName(null);
        setIdentityDescription(null);
        setSecurity(Integer.MIN_VALUE);
        mIdentityHeaderFragment.updateUI(name, description, security);
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

        String check = ((BaseIdentityFragment) getFragmentManager().findFragmentById(R.id.fragment_container_content)).check();
        if (check == null) {

            if (step == fragments.length - 1) {
                getToMainFragment();

                return;
            }
            step++;
            mIdentityHeaderFragment.updateUI(name, description, security);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null).commit();
            updateActionBar(step);
        } else {
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info, R.string.create_identity_enter_all_data, R.string.ok);
        }
    }

    private void updateActionBar(int step) {

        if (step < fragments.length - 1) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            mActionNext.setTitle(R.string.next);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            mActionNext.setTitle(R.string.finish);
        }
    }

    private void completed() {

        setResult(RESULT_OK);
        finish();
    }

    public void setIdentityDescription(String text) {

        description = text;
    }

    public void setIdentityName(String text) {

        name = text;
    }

    public void setSecurity(int progress) {

        security = progress;
    }
}
