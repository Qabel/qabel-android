package de.qabel.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.qabel.android.R;
import de.qabel.android.activities.BaseWizardActivity;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountPasswordFragment extends BaseIdentityFragment {

    private EditText etPassword1;
    private EditText etPassword2;
    private BaseWizardActivity.NextChecker mChecker;

    public static CreateAccountPasswordFragment newInstance(BaseWizardActivity.NextChecker checker) {

        CreateAccountPasswordFragment fragment = new CreateAccountPasswordFragment();
        fragment.mChecker = checker;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_password, container, false);

        etPassword1 = ((EditText) view.findViewById(R.id.et_password1));
        etPassword2 = (EditText) view.findViewById(R.id.et_password2);
        return view;
    }

    @Override
    public String check() {

        if (etPassword1.getText().length() < 3) {
            return getString(R.string.password_to_short);
        }
        //check if pw1 match pw2
        if (etPassword1.getText().toString().equals(etPassword2.getText().toString())) {
            //yes, check password
            return mChecker.check(etPassword1);
        } else {
            //no
            return getString(R.string.create_account_passwords_dont_match);
        }
    }
}
