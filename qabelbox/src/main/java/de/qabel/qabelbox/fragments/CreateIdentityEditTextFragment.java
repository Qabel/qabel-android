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
import de.qabel.qabelbox.activities.BaseWizardActivity;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityEditTextFragment extends BaseIdentityFragment {

    private EditText editText;
    private int mMessageId;
    private int mEditTextHintId;
    private Integer inputType;
    private BaseWizardActivity.NextChecker mChecker;
    private boolean optionalValue;

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker) {
        return newInstance(messageId, editTextHintId, checker, null);
    }

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker, Integer textInputType) {
        return newInstance(messageId, editTextHintId, checker, textInputType, false);
    }

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker, Integer textInputType, boolean optinal) {

        CreateIdentityEditTextFragment fragment = new CreateIdentityEditTextFragment();
        fragment.mMessageId = messageId;
        fragment.mEditTextHintId = editTextHintId;
        fragment.mChecker = checker;
        fragment.optionalValue = optinal;
        if (textInputType != null) {
            fragment.inputType = textInputType;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_edittext, container, false);

        TextView tvMessage = ((TextView) view.findViewById(R.id.tv_message));
        editText = (EditText) view.findViewById(R.id.et_name);
        tvMessage.setText(mMessageId);
        if (!optionalValue) {
            editText.setHint(mEditTextHintId);
        } else {
            editText.setHint(getString(mEditTextHintId) + " (" + getString(R.string.optional) + ")");
        }
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
