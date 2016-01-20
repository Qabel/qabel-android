package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityMainFragment extends BaseIdentityFragment implements View.OnClickListener {

    Button mCreateIdentity;
    Button mImportIdentity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_main, container, false);
        mCreateIdentity = (Button) view.findViewById(R.id.bt_create_identity);
        mImportIdentity = (Button) view.findViewById(R.id.bt_import_identity);
        mCreateIdentity.setOnClickListener(this);
        mImportIdentity.setOnClickListener(this);
        return view;
    }

    @Override
    public String check() {

        return null;
    }

    @Override
    public void onClick(View v) {

        if (v == mCreateIdentity) {
            mActivty.handleNextClick();
        }
        if (v == mImportIdentity) {
            UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.function_not_yet_implenented);
        }
    }
}

