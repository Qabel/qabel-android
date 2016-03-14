package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.SimpleJsonCallback;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.validation.PasswordValidator;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by danny on 19.01.16.
 */
public class ChangeBoxAccountPasswordFragment extends Fragment {

    private EditText etOldPassword, etPassword1, etPassword2;
    private final BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();
    private PasswordValidator validator = new PasswordValidator();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_box_account_password, container, false);

        etOldPassword = (EditText) view.findViewById(R.id.et_old_password);
        etPassword1 = (EditText) view.findViewById(R.id.et_password1);
        etPassword2 = (EditText) view.findViewById(R.id.et_password2);
        setHasOptionsMenu(true);
        //tvMessage.setText(mMessageId);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_ok) {
            String pw = etPassword1.getText().toString();
            String pwRepeat = etPassword2.getText().toString();

            Integer check = validator.validate(null, pw, pwRepeat);
            if (check != null) {
                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, getString(check));
                return true;
            } else {
                sendChangePWRequest(etOldPassword.getText().toString(), etPassword1.getText().toString(), etPassword2.getText().toString());
                return true;
            }
        }
        return false;
    }

    private void sendChangePWRequest(final String oldPassword, final String newPassword1, final String newPassword2) {

        final AlertDialog dialog = UIHelper.showWaitMessage(getActivity(), R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);

        final SimpleJsonCallback callback = createCallback(oldPassword, newPassword1, newPassword2, dialog);

        mBoxAccountServer.changePassword(getActivity(), oldPassword, newPassword1, newPassword2, callback);
    }

    @NonNull
    private SimpleJsonCallback createCallback(final String oldPassword, final String newPassword1, final String newPassword2, final AlertDialog dialog) {

        return new SimpleJsonCallback() {

            void showRetryDialog() {

                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.server_access_not_successfully_retry_question, R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        sendChangePWRequest(oldPassword, newPassword1, newPassword2);
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
                    mBoxAccountServer.changePassword(getActivity(), oldPassword, newPassword1, newPassword2, this);
                } else {
                    dialog.dismiss();
                    showRetryDialog();
                }
            }

            protected void onSuccess(Call call, Response response, JSONObject json) {

                final BoxAccountRegisterServer.ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                if (result.success != null) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            dialog.dismiss();
                            getActivity().onBackPressed();
                            Toast.makeText(getActivity(), result.success, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else

                {

                    String errorText = generateErrorMessage(result);
                    dialog.dismiss();
                    UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, errorText);
                }
            }

            private String generateErrorMessage(BoxAccountRegisterServer.ServerResponse result) {

                ArrayList<String> message = new ArrayList<>();
                if (result.non_field_errors != null) {
                    message.add(result.non_field_errors);
                }
                if (result.old_password != null) {
                    message.add(result.old_password);
                }
                if (result.new_password1 != null) {
                    message.add(result.new_password1);
                }
                if (result.new_password2 != null) {
                    message.add(result.new_password2);
                }

                String errorText;
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
}

