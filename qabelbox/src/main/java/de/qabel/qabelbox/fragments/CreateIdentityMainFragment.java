package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityMainFragment extends BaseIdentityFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_main, container, false);
        return view;
    }

    @Override
    public String check() {

        return null;
    }
}
