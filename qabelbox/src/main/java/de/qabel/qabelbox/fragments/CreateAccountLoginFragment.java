package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.BaseWizwardActivity;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.SimpleJsonCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.UIHelper;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountLoginFragment extends BaseIdentityFragment {

    private EditText etPassword;
    private BaseWizwardActivity.NextChecker mChecker;
    private TextView etUserName;

    BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_login, container, false);
        etUserName = ((TextView) view.findViewById(R.id.et_username));
        etPassword = (EditText) view.findViewById(R.id.et_password);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_ok) {
            login(etUserName.getText().toString(), etPassword.getText().toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void login(final String username, final String password) {

        final AlertDialog dialog = UIHelper.showWaitMessage(mActivty, R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);

        final SimpleJsonCallback callback = new SimpleJsonCallback() {

            void showRetryDialog() {

                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.server_access_not_successfully_retry_question, R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        login(username, password);
                    }
                }
                        , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
            }

            protected void onError(final Call call, SimpleJsonCallback.Reasons reasons) {

                if (reasons == Reasons.IOException && retryCount++ < 3) {
                    call.enqueue(this);
                } else {
                    dialog.dismiss();
                    showRetryDialog();
                }
            }

            protected void onSuccess(Call call, Response response, JSONObject json) {

                BoxAccountRegisterServer.ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                if (result.token != null && result.token.length() > 5) {
                    new AppPreference(getActivity()).setToken(result.token);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            dialog.dismiss();
                            mActivty.completeWizard();
                        }
                    });
                } else {
                    if(result.non_field_errors!=null)
                    {
                        dialog.dismiss();
                        UIHelper.showDialogMessage(getActivity(),R.string.dialog_headline_info,result.non_field_errors);
                    }
                }
            }
        };

        mBoxAccountServer.login(username, password, callback);
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
    }

    @Override
    public String check() {

        UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.function_not_yet_implenented);
        //return mChecker.check(editText);
        return null;
    }
}
