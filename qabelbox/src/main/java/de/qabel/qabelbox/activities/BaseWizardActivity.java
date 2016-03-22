package de.qabel.qabelbox.activities;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.qabel.core.accounting.AccountingHTTP;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateIdentityHeaderFragment;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 11.01.2016.
 */
public abstract class BaseWizardActivity extends CrashReportingActivity {

    private String TAG = this.getClass().getSimpleName();

    public static final String FIRST_RUN = "first_run";

    public static final String P_IDENTITY = "identity_name";
    protected BaseWizardActivity mActivity;
    protected MenuItem mActionNext;
    private ActionBar actionBar;
    private CreateIdentityHeaderFragment mIdentityHeaderFragment;

    protected BaseIdentityFragment[] fragments;
    protected int step = 0;
    public int activityResult = RESULT_CANCELED;
    protected boolean mFirstRun;
    //values for create box account mode
    AccountingHTTP mAccounting;
    protected boolean canExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity = this;
        mFirstRun = getIntent().getBooleanExtra(FIRST_RUN, true);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_create_identity);
        setupToolbar();
        createFragments();
        actionBar.setTitle(getActionBarTitle());
    }

    private void setupToolbar() {

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();
        assert getSupportActionBar() != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        if (!canExit) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onBackPressed();
            }
        });
    }

    private void createFragments() {

        mIdentityHeaderFragment = new CreateIdentityHeaderFragment();
        fragments = getFragmentList();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container_content, fragments[0]);
        ft.add(R.id.fragment_container_header, mIdentityHeaderFragment);
        ft.commit();
    }

    @Override
    public void onBackPressed() {

        int fragmentCount = getFragmentManager().getBackStackEntryCount();
        //check backstack if fragments exists
        if (step < fragments.length && fragmentCount > 0) {

            //check if last fragment displayed
            if (fragmentCount == fragments.length - 1) {
                //complete wizard
                activityResult = RESULT_OK;
                completeWizard();

                return;
            }
            //otherwise, popbackstack and update ui
            if (step > 0) {
                step--;
            }
            if (step == 0 && !canExit) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayUseLogoEnabled(true);
            }
            getFragmentManager().popBackStack();
            mIdentityHeaderFragment.updateUI(getHeaderFragmentText());
            updateActionBar(step);
        } else {
            //return without finish the wizard
            if (canExit) {
                finish();
            } else {

                UIHelper.showDialogMessage(this, getString(R.string.dialog_headline_warning), String.format(getString(R.string.message_step_is_needed_or_close_app), getWizardEntityLabel()), R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        finish();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
            }
        }
    }

    protected abstract String getHeaderFragmentText();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.ab_create_identity, menu);
        mActionNext = menu.findItem(R.id.action_next);
        updateActionBar(step);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_next) {
            handleNextClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void handleNextClick() {

        String check = ((BaseIdentityFragment) getFragmentManager().findFragmentById(R.id.fragment_container_content)).check();
        //check if fragment ready to go to the next step
        if (check != null) {
            //no, show error message
            UIHelper.showDialogMessage(this, R.string.dialog_headline_info, check);
        } else {

            //check if currently last step
            if (step == fragments.length - 1) {
                activityResult = RESULT_OK;
                completeWizard();
                return;
            }
            //no... go to next step

            if (canShowNext(step)) {
                showNextFragment();
            }
        }
    }

    /**
     * override this if you need special handling on next click
     *
     * @param step
     * @return
     */
    protected boolean canShowNext(int step) {
        return true;
    }

    protected void showNextFragment() {

        step++;
        if (step == fragments.length - 1) {
            canExit = true;
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);

        mIdentityHeaderFragment.updateUI(getHeaderFragmentText());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragments[step]).addToBackStack(null).commit();
        updateActionBar(step);
    }

    /**
     * refresh actionbar depends from current wizard state
     *
     * @param step current step number
     */
    protected void updateActionBar(int step) {

        //update icons
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

        //update subtitle
        if (step == 0) {
            actionBar.setSubtitle(null);
        } else if (step < fragments.length - 1) {
            actionBar.setSubtitle(getString(R.string.step_x_from_y).replace("$1", step + "").replace("$2", (fragments.length - 2) + ""));
        }
    }

    protected abstract int getActionBarTitle();

    protected abstract BaseIdentityFragment[] getFragmentList();

    public abstract void completeWizard();

    public interface NextChecker {

        String check(View view);
    }

    protected abstract String getWizardEntityLabel();
}
