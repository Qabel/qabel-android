package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.activities.BaseWizardActivity;
import de.qabel.qabelbox.activities.BaseWizardActivity.NextChecker;

public class CreateIdentityEditTextFragment extends BaseIdentityFragment {

    private EditText editText;
    private int mMessageId;
    private int mEditTextHintId;
    private Integer inputType;
    private NextChecker mChecker;

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, NextChecker checker) {
        return newInstance(messageId, editTextHintId, checker, null);
    }

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, NextChecker checker, Integer textInputType) {

        CreateIdentityEditTextFragment fragment = new CreateIdentityEditTextFragment();
        fragment.mMessageId = messageId;
        fragment.mEditTextHintId = editTextHintId;
        fragment.mChecker = checker;
        if (textInputType != null) {
            fragment.inputType = textInputType;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(layout.fragment_create_identity_edittext, container, false);

        TextView tvMessage = (TextView) view.findViewById(id.tv_message);
        editText = (EditText) view.findViewById(id.et_name);
        tvMessage.setText(mMessageId);
        editText.setHint(mEditTextHintId);
        if (inputType != null) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | inputType);
        }
        return view;
    }

    @Override
    public String check() {

        return mChecker.check(editText);
    }
}
