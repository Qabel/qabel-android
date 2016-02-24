package de.qabel.qabelbox.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.SimpleJsonCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateAccountFinalFragment;
import de.qabel.qabelbox.fragments.CreateAccountMainFragment;
import de.qabel.qabelbox.fragments.CreateAccountPasswordFragment;
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment;
import de.qabel.qabelbox.helper.Formatter;
import de.qabel.qabelbox.helper.UIHelper;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by danny on 11.01.2016.
 */
public class CreateAccountActivity extends BaseWizardActivity {

    private final String TAG = this.getClass().getSimpleName();

    private String mBoxAccountName;

    private String mBoxAccountPassword1;
    private String mBoxAccountPassword2;
    private String mBoxAccountEMail;

    private final BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();

    @Override
    protected String getHeaderFragmentText() {

        return mBoxAccountName;
    }

    @Override
    protected int getActionBarTitle() {

        return R.string.headline_create_box_account;
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
                setPassword(editText, editText);
                return null;
            }
        });

        //final fragment
        fragments[4] = new CreateAccountFinalFragment();
        return fragments;
    }

    private String checkEMailAddress(String editText) {

        boolean error = editText.length() < 1;
        if (error) {
            return getString(R.string.create_identity_enter_all_data);
        }
        if (!Formatter.isEMailValid(editText)) {
            return getString(R.string.email_adress_invalid);
        }
        return null;
    }

    private void setEMail(String editText) {

        this.mBoxAccountEMail = editText;
    }

    private String checkBoxAccountName(String editText) {

        if (editText.length() < 1) {
            return getString(R.string.create_account_enter_all_data);
        }
        if (editText.length() > 32) {
            return getString(R.string.create_account_maximum_32_chars);
        }

        return null;
    }

    @Override
    public void completeWizard() {

        if (QabelBoxApplication.getInstance().getService().getIdentities().getIdentities().size() > 0) {
            //fallback if identity exists after box account created. this case should never thrown
            Log.e(TAG, "Identity exist after create box account");
            Toast.makeText(mActivity, R.string.skip_create_identity, Toast.LENGTH_SHORT).show();

            finish();
            Intent i = new Intent(mActivity, MainActivity.class);
            startActivity(i);
            return;
        }
        Intent i = new Intent(mActivity, CreateIdentityActivity.class);
        i.putExtra(BaseWizardActivity.FIRST_RUN, true);
        finish();
        startActivity(i);
    }

    private void setAccountName(String text) {

        mBoxAccountName = text;
    }

    private void register(final String username, final String password1, final String password2, final String email) {

        final AlertDialog dialog = UIHelper.showWaitMessage(this, R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);

        final SimpleJsonCallback callback = createCallback(username, password1, password2, email, dialog);

        mBoxAccountServer.register(username, password1, password2, email, callback);
    }

    @NonNull
    private SimpleJsonCallback createCallback(final String username, final String password1, final String password2, final String email, final AlertDialog dialog) {

        return new SimpleJsonCallback() {

            void showRetryDialog() {

                UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_info, R.string.server_access_not_successfully_retry_question, R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        register(username, password1, password2, email);
                    }
                }
                        , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
            }

            protected void onError(final Call call, Reasons reasons) {

                if (reasons == Reasons.IOException && retryCount++ < 3) {
                    mBoxAccountServer.register(username, password1, password2, email, this);
                } else {
                    dialog.dismiss();
                    showRetryDialog();
                }
            }

            protected void onSuccess(Call call, Response response, JSONObject json) {

                BoxAccountRegisterServer.ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                if (result.token != null && result.token.length() > 5) {
                    new AppPreference(mActivity).setToken(result.token);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            dialog.dismiss();
                            mActivity.showNextFragment();
                        }
                    });
                } else {
                    String errorText = generateErrorMessage(result);
                    dialog.dismiss();
                    UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_info, errorText);
                }
            }

            private String generateErrorMessage(BoxAccountRegisterServer.ServerResponse result) {

                ArrayList<String> message = new ArrayList<>();
                if (result.non_field_errors != null) {
                    message.add(result.non_field_errors);
                }
                if (result.password1 != null) {
                    message.add(result.password1);
                }
                if (result.password2 != null) {
                    message.add(result.password2);
                }
                if (result.email != null) {
                    message.add(result.email);
                }
                if (result.username != null) {
                    message.add(result.username);
                }
                String errorText = "";
                if (message.size() == 0) {
                    errorText = getString(R.string.server_access_failed_or_invalid_check_internet_connection);
                } else {
                    errorText = "- " + message.get(0);
                    for (int i = 1; i < message.size(); i++) {
                        errorText += "\n -" + message.get(i);
                    }
                }
                return errorText;
            }
        };
    }

    @Override
    protected void updateActionBar(int step) {

        super.updateActionBar(step);
        //override next button text with create identity
        if (step == fragments.length - 1) {
            mActionNext.setTitle(R.string.btn_create_identity);
        }
    }

    @Override
    protected boolean canShowNext(int step) {

        if (step == fragments.length - 2) {
            register(mBoxAccountName, mBoxAccountPassword1, mBoxAccountPassword2, mBoxAccountEMail);
            return false;
        } else {
            return true;
        }
    }

    private void setPassword(String password1, String password2) {

        mBoxAccountPassword1 = password1;
        mBoxAccountPassword2 = password2;
    }
}
