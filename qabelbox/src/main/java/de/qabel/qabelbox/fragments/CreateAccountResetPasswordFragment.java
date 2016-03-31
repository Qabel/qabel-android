package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.*;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.SimpleJsonCallback;
import de.qabel.qabelbox.helper.UIHelper;
import okhttp3.Call;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountResetPasswordFragment extends BaseIdentityFragment {

    private TextView etEMail;
    private final BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
        savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_reset_password, container, false);
        etEMail = ((TextView) view.findViewById(R.id.et_email));

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
            String check = checkData();
            if (check != null) {
                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, check);
                return true;
            }
            resetPassword(etEMail.getText().toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String checkData() {

        if (etEMail.getText().toString().length() < 3 || etEMail.getText().toString().length() == 0) {
            return getString(R.string.create_account_enter_all_data);
        }
        return null;
    }

    private void resetPassword(final String email) {

        final AlertDialog dialog = UIHelper.showWaitMessage(mActivity, R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);

        final SimpleJsonCallback callback = createCallback(email, dialog);

        mBoxAccountServer.resetPassword(email, callback);
    }

    @NonNull
    private SimpleJsonCallback createCallback(final String email, final AlertDialog dialog) {

        return new SimpleJsonCallback() {

            void showRetryDialog() {

                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.server_access_not_successfully_retry_question, R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            resetPassword(email);
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
                    mBoxAccountServer.resetPassword(email, this);
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
                            UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, result.success);
                        }
                    });
                } else {
                    String text = generateErrorMessage(result);
                    dialog.dismiss();
                    UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, text);
                }
            }

            private String generateErrorMessage(BoxAccountRegisterServer.ServerResponse result) {

                ArrayList<String> message = new ArrayList<>();
                if (result.non_field_errors != null) {
                    message.add(result.non_field_errors);
                }
                if (result.email != null) {
                    message.add(result.email);
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
}
