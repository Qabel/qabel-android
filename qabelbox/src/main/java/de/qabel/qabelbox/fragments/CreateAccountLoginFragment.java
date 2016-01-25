package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.BaseWizwardActivity;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountLoginFragment extends BaseIdentityFragment {

    private EditText editText;
    private int mMessageId;
    private int mEditTextHintId;
    private BaseWizwardActivity.NextChecker mChecker;
    private TextView tvMessage;

    public static CreateAccountLoginFragment newInstance(int messageId, int editTextHintId, BaseWizwardActivity.NextChecker checker) {

        CreateAccountLoginFragment fragment = new CreateAccountLoginFragment();
        fragment.mMessageId = messageId;
        fragment.mEditTextHintId = editTextHintId;
        fragment.mChecker = checker;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_login, container, false);

        tvMessage = ((TextView) view.findViewById(R.id.et_name));
        editText = (EditText) view.findViewById(R.id.et_password);
        tvMessage.setText(mMessageId);
        editText.setHint(mEditTextHintId);
        return view;
    }

    @Override
    public String check() {
        UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.function_not_yet_implenented);
        //return mChecker.check(editText);
        return null;
    }
}
