package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.activities.BaseWizardActivity.NextChecker;
import de.qabel.qabelbox.validation.PasswordValidator;

public class CreateAccountPasswordFragment extends BaseIdentityFragment {
    private EditText etPassword1;
    private EditText etPassword2;
    private NextChecker mChecker;

    private String accountName;

    private PasswordValidator validator = new PasswordValidator();

    public static CreateAccountPasswordFragment newInstance(NextChecker checker) {
        CreateAccountPasswordFragment fragment = new CreateAccountPasswordFragment();
        fragment.mChecker = checker;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(layout.fragment_create_account_password, container, false);

        etPassword1 = (EditText) view.findViewById(id.et_password1);
        etPassword2 = (EditText) view.findViewById(id.et_password2);
        return view;
    }

    @Override
    public String check() {
        Integer validationMessage = validator.validate(accountName,
                etPassword1.getText().toString(), etPassword2.getText().toString());
        //check if pw1 match pw2
        if (validationMessage == null) {
            //yes, check password
            return mChecker.check(etPassword1);
        }
        return getString(validationMessage);
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
