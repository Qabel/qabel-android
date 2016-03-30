package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountMainFragment extends BaseIdentityFragment implements View.OnClickListener {

    private Button mCreateAccount;
    private Button mLogin;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_main, container, false);
        mCreateAccount = (Button) view.findViewById(R.id.bt_create_box_account);
        mLogin = (Button) view.findViewById(R.id.bt_login);
        mCreateAccount.setOnClickListener(this);
        mLogin.setOnClickListener(this);
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
            mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mActivity.getSupportActionBar().setDisplayUseLogoEnabled(false);

            CreateAccountLoginFragment fragment = new CreateAccountLoginFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragment).addToBackStack(null).commit();
        }
    }

}

