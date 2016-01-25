package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateAccountFinalFragment;
import de.qabel.qabelbox.fragments.CreateAccountMainFragment;
import de.qabel.qabelbox.fragments.CreateAccountPasswordFragment;
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment;

/**
 * Created by danny on 11.01.2016.
 */
public class CreateAccountActivity extends BaseWizwardActivity {

    private String TAG = this.getClass().getSimpleName();

    private String mBoxAccountName;

    private Identity mBoxAccountEMail;
    private String mBoxAccountPassword;

    @Override
    protected String getHeaderFragmentText() {

        return mBoxAccountName;
    }

    @Override
    protected int getActionBarTitle() {

        return R.string.headline_add_identity;
    }

    /**
     * fill fragment list with fragments to navigate via wizard
     */
    @Override
    protected BaseIdentityFragment[] getFragmentList() {

        BaseIdentityFragment fragments[] = new BaseIdentityFragment[5];
        //main fragment with login and registrate new account button
        fragments[0] = new CreateAccountMainFragment();

        //enter box name fragment
        fragments[1] = CreateIdentityEditTextFragment.newInstance(R.string.create_account_enter_name_infos, R.string.create_account_name_hint, new NextChecker() {
            @Override
            public String check(View view) {

                String editText = ((EditText) view).getText().toString().trim();
                String result = checkBoxAccountName(editText);
                if (result != null) {
                    return result;
                }
                result = checkBoxAccountNameByServer(editText);
                if (result != null) {
                    return result;
                }
                setAccountName(editText);
                return null;
            }
        });

        //enter email address fragment
        fragments[2] = CreateIdentityEditTextFragment.newInstance(R.string.create_account_email, R.string.create_account_email_hint, new NextChecker() {
            @Override
            public String check(View view) {
                String editText = ((EditText) view).getText().toString().trim();
                String result = checkEMailAddress(editText);
                if (result != null) {
                    return result;
                }

                setEMail(editText);
                return null;
            }
        });

        //enter email address fragment
        fragments[3] = CreateAccountPasswordFragment.newInstance(new NextChecker() {
            @Override
            public String check(View view) {
                String editText = ((EditText) view).getText().toString().trim();
                String result = checkPW(editText);
                if (result == null) {
                    setPassword(editText);
                    return null;
                } else {
                    return result;
                }
            }
        });

        //final fragment
        fragments[4] = new CreateAccountFinalFragment();
        return fragments;
    }

    private String checkPW(String editText) {
        //@todo check if password is valid. check all data, length, minmax ....
        return null;
    }

    private String checkEMailAddress(String editText) {
        boolean error = editText.length() < 1;
        if (error) {
            return getString(R.string.create_identity_enter_all_data);
        }
        //@todo regex check email
        return null;
    }

    private void setEMail(String editText) {

    }

    private String checkBoxAccountNameByServer(String editText) {
        //@todo check username with server
        return null;
    }

    private String checkBoxAccountName(String editText) {
        if (editText.length() < 1) {
            return getString(R.string.create_account_enter_all_data);
        }
        if (editText.length() > 32) {
            return getString(R.string.create_account_maximum_32_chars);
        }
        if (!checkBoxAccountNameCharacter(editText)) {
            return getString(R.string.create_account_invalid_characters);
        }
        return null;
    }

    boolean checkBoxAccountNameCharacter(String editText) {
        //@todo: check name regex needed
        return true;
    }

    @Override
    protected void completeWizard() {

        if (mFirstRun) {
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.setAction("");
            startActivity(intent);
        }
        else {
            Intent result = new Intent();
       //     result.putExtra(P_IDENTITY, mNewIdentity);
            setResult(activityResult, result);
            finish();
        }
    }

    public void setAccountName(String text) {

        mBoxAccountName = text;
    }


    public void setPassword(String password) {
        mBoxAccountPassword = password;
    }
}
