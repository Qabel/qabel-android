package de.qabel.qabelbox.fragments;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.qabel.qabelbox.R;

public class CreateAccountMainFragment extends BaseIdentityFragment implements View.OnClickListener {

    private Button mCreateAccount;
    private Button mLogin;
    private boolean skipToLogin = false;

    public static String SKIP_TO_LOGIN = "SKIP_TO_LOGIN";
    private String accountName;
    private String accountEmail;


    @Override
    public void setArguments(Bundle args) {
        skipToLogin = args.getBoolean(SKIP_TO_LOGIN, false);
        accountName = args.getString(ACCOUNT_NAME);
        accountEmail = args.getString(ACCOUNT_EMAIL);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_account_main, container, false);
        if (skipToLogin) {
            startLogin();
        } else {
            mCreateAccount = (Button) view.findViewById(R.id.bt_create_box_account);
            mLogin = (Button) view.findViewById(R.id.bt_login);
            mCreateAccount.setOnClickListener(this);
            mLogin.setOnClickListener(this);
        }
        return view;
    }

    @Override
    public String check() {

        return null;
    }

    @Override
    public void onClick(View v) {

        if (v == mCreateAccount) {
            mActivity.handleNextClick();
        }
        if (v == mLogin) {
            startLogin();
        }
    }

    public void startLogin() {
        CreateAccountLoginFragment fragment = new CreateAccountLoginFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ACCOUNT_NAME, accountName);
        bundle.putString(ACCOUNT_EMAIL, accountEmail);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragment);
        if(accountName == null || accountName.isEmpty()){
            transaction.addToBackStack(CreateAccountLoginFragment.class.getSimpleName());
        }
        transaction.commit();
    }

}

