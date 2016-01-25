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

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityEditTextFragment extends BaseIdentityFragment {

    private EditText editText;
    private int mMessageId;
    private int mEditTextHintId;
    private BaseWizwardActivity.NextChecker mChecker;
    private TextView tvMessage;

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizwardActivity.NextChecker checker) {

        CreateIdentityEditTextFragment fragment = new CreateIdentityEditTextFragment();
        fragment.mMessageId = messageId;
        fragment.mEditTextHintId = editTextHintId;
        fragment.mChecker = checker;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_edittext, container, false);

        tvMessage = ((TextView) view.findViewById(R.id.tv_message));
        editText = (EditText) view.findViewById(R.id.et_name);
        tvMessage.setText(mMessageId);
        editText.setHint(mEditTextHintId);
        return view;
    }

    @Override
    public String check() {

        return mChecker.check(editText);
    }
}
