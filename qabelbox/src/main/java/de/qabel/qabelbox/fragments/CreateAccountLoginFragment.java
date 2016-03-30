package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer.ServerResponse;
import de.qabel.qabelbox.communication.callbacks.SimpleJsonCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.UIHelper;
import okhttp3.Call;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.ArrayList;

public class CreateAccountLoginFragment extends BaseIdentityFragment {
    private EditText etPassword;
    private TextView etUserName;

    private final BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();
    private View resetPassword;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(layout.fragment_create_account_login, container, false);
        etUserName = (TextView) view.findViewById(id.et_username);
        etPassword = (EditText) view.findViewById(id.et_password);
        resetPassword = view.findViewById(id.reset_password);

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                CreateAccountResetPasswordFragment fragment = new CreateAccountResetPasswordFragment();
                getActivity().getFragmentManager().beginTransaction().replace(id.fragment_container_content, fragment).addToBackStack(null).commit();
            }
        });
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(menu.ab_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == id.action_ok) {
            String check = checkData();
            if (check != null) {
                UIHelper.showDialogMessage(getActivity(), string.dialog_headline_info, check);
                return true;
            }
            login(etUserName.getText().toString(), etPassword.getText().toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String checkData() {
        if (etPassword.getText().toString().length() < 3 || etUserName.getText().toString().length() == 0) {
            return getString(string.create_account_enter_all_data);
        }
        return null;
    }

    private void login(final String username, final String password) {
        final AlertDialog dialog = UIHelper.showWaitMessage(mActivity, string.dialog_headline_please_wait, string.dialog_message_server_communication_is_running, false);
        final SimpleJsonCallback callback = createCallback(username, password, dialog);
        mBoxAccountServer.login(username, password, callback);
    }

    @NonNull
    private SimpleJsonCallback createCallback(final String username, final String password, final AlertDialog dialog) {
        return new SimpleJsonCallback() {
            void showRetryDialog() {
                UIHelper.showDialogMessage(getActivity(), string.dialog_headline_info, string.server_access_not_successfully_retry_question, string.yes, string.no, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                login(username, password);
                            }
                        }
                        , new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }

            @Override
            protected void onError(final Call call, Reasons reasons) {
                if (reasons == Reasons.IOException && retryCount++ < 3) {
                    mBoxAccountServer.login(username, password, this);
                } else {
                    dialog.dismiss();
                    showRetryDialog();
                }
            }

            @Override
            protected void onSuccess(Call call, Response response, JSONObject json) {
                ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                if (result.token != null && result.token.length() > 5) {
                    new AppPreference(getActivity()).setToken(result.token);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            mActivity.completeWizard();
                        }
                    });
                } else {
                    String text = generateErrorMessage(result);
                    dialog.dismiss();
                    UIHelper.showDialogMessage(getActivity(), string.dialog_headline_info, text);
                }
            }

            private String generateErrorMessage(ServerResponse result) {
                ArrayList<String> message = new ArrayList<>();
                if (result.non_field_errors != null) {
                    message.add(result.non_field_errors);
                }
                if (result.password != null) {
                    message.add(result.password);
                }
                if (result.username != null) {
                    message.add(result.username);
                }

                String errorText;
                if (message.size() == 0) {
                    errorText = getString(string.server_access_failed_or_invalid_check_internet_connection);
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
    public String check() {
        UIHelper.showDialogMessage(getActivity(), string.dialog_headline_info, string.function_not_yet_implenented);
        return null;
    }
}
