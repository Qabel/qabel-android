package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityDescriptionFragment extends BaseIdentityFragment {

    private EditText tvDescription;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_description, container, false);
        tvDescription = (EditText) view.findViewById(R.id.et_description);
        return view;
    }

    @Override
    public String check() {

        String text = tvDescription.getText().toString();
        boolean error =
                text.length() < 4;
        if (error) {
            return getString(R.string.create_identity_enter_all_data);
        }
        mActivty.setIdentityDescription(text);
        return null;
    }

    @Override
    public void resetData() {

        if (tvDescription != null)
            tvDescription.setText("");
    }
    @Override
    public void onBackPressed() {

        super.onBackPressed();
        mActivty.setIdentityDescription(null);
    }
}

