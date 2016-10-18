package de.qabel.qabelbox.startup.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.startup.activities.BaseWizardActivity;

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

    private String value;

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker) {
        return newInstance(messageId, editTextHintId, checker, null);
    }

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker, Integer textInputType) {
        return newInstance(messageId, editTextHintId, checker, textInputType, false);
    }

    public static CreateIdentityEditTextFragment newInstance(int messageId, int editTextHintId, BaseWizardActivity.NextChecker checker, Integer textInputType, boolean optional) {

        CreateIdentityEditTextFragment fragment = new CreateIdentityEditTextFragment();
        fragment.mMessageId = messageId;
        fragment.mEditTextHintId = editTextHintId;
        fragment.mChecker = checker;
        fragment.optionalValue = optional;
        if (textInputType != null) {
            fragment.inputType = textInputType;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_create_identity_edittext, container, false);

        TextView tvMessage = ((TextView) view.findViewById(R.id.tv_message));
        tvMessage.setText(mMessageId);

        editText = (EditText) view.findViewById(R.id.edit_text);
        if (value != null) {
            editText.setText(value);
        }
        if (!optionalValue) {
            editText.setHint(mEditTextHintId);
        } else {
            editText.setHint(getString(mEditTextHintId) + " (" + getString(R.string.optional) + ")");
        }
        if (inputType != null) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | inputType);
        }
        editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof BaseWizardActivity) {
                        ((BaseWizardActivity) activity).handleNextClick();
                        return true;
                    }
                }
                return false;
            }
        });
        return view;
    }

    public void setValue(String value) {
        this.value = value;
        if (editText != null) {
            editText.setText(value);
        }
    }

    @Override
    public String check() {
        return mChecker.check(editText);
    }
}
