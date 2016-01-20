package de.qabel.qabelbox.activities;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.qabel.core.accounting.AccountingHTTP;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
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

    public static String FIRST_RUN = "first_run";
    public static String MODE = "mode";
    public static String MODE_IDENTITY = "mode_identity";
    public static String P_IDENTITY = "identity_name";
    private CreateIdentityActivity mActivity;
    private Toolbar mToolbar;
    MenuItem mActionNext;
    private ActionBar actionBar;
    private CreateIdentityHeaderFragment mIdentityHeaderFragment;
    private CreateIdentityNameFragment mIdentityNameFragment;
    String name;
    int security = Integer.MIN_VALUE;

    BaseIdentityFragment[] fragments;
    int step = 0;
    private String TAG = this.getClass().getSimpleName();
    AccountingHTTP mAccounting;
    int activityResult = RESULT_CANCELED;
    boolean mFirstRun;
    private Identity mNewIdentity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;
        mFirstRun = getIntent().getBooleanExtra(FIRST_RUN, true);
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
        fillFragmentList();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container_content, fragments[0]);
        ft.add(R.id.fragment_container_header, mIdentityHeaderFragment);
        ft.commit();
    }

    private void fillFragmentList() {

        fragments = new BaseIdentityFragment[4];
        fragments[0] = new CreateIdentityMainFragment();
        fragments[1] = new CreateIdentityNameFragment();
        fragments[2] = new CreateIdentitySecurityFragment();
        fragments[3] = new CreateIdentityFinalFragment();
    }

    @Override
    public void onBackPressed() {

        System.out.println("onback " + getFragmentManager().getBackStackEntryCount());
        int fragmentCount = getFragmentManager().getBackStackEntryCount();
        if (step < fragments.length && fragmentCount > 0) {
            if (fragmentCount == fragments.length - 1) {
                activityResult = RESULT_OK;
                finishWizard();
                return;
            }
            ((BaseIdentityFragment) getFragmentManager().findFragmentById(R.id.fragment_container_content)).onBackPressed();
            getFragmentManager().popBackStack();
            step--;
            mIdentityHeaderFragment.updateUI(name);
            return;
        } else {
            finish();
        }
    }

    private void finishWizard() {

        Intent result = new Intent();
        result.putExtra(P_IDENTITY, mNewIdentity);
        setResult(activityResult, result);

        finish();
        if (mFirstRun) {
            Intent i = new Intent(mActivity, MainActivity.class);
            i.setAction("");
            startActivity(i);
        }
        //getToMainFragment();
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
        ft.commit();
        resetUI();

        updateActionBar(step);
    }

    private void resetUI() {

        setIdentityName(null);
        setSecurity(Integer.MIN_VALUE);
        mIdentityHeaderFragment.updateUI(name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.ab_create_identity, menu);
        mActionNext = menu.findItem(R.id.action_next);
        updateActionBar(step);
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

    public void handleNextClick() {

        String check = ((BaseIdentityFragment) getFragmentManager().findFragmentById(R.id.fragment_container_content)).check();
        if (check == null) {

            if (step == fragments.length - 1) {
                activityResult = RESULT_OK;

                finishWizard();

                return;
            }
            step++;
            mIdentityHeaderFragment.updateUI(name);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null).commit();
            updateActionBar(step);
        } else {
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info, R.string.create_identity_enter_all_data, R.string.ok);
        }
    }

    private void updateActionBar(int step) {

        if (step == 0) {
            mActionNext.setVisible(false);
        } else if (step < fragments.length - 1) {
            mActionNext.setVisible(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            mActionNext.setTitle(R.string.next);
        } else {
            mActionNext.setVisible(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            mActionNext.setTitle(R.string.finish);
        }
    }

    public void setIdentityName(String text) {

        name = text;
    }

    public void setSecurity(int progress) {

        security = progress;
    }

    public String getIdentityName() {

        return name;
    }

    public int getSecurity() {

        return security;
    }

    public void setCreatedIdentity(Identity identity) {

        mNewIdentity = identity;
    }
}
