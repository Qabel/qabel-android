package de.qabel.qabelbox.startup.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.qabel.qabelbox.R;

public class CreateIdentityFinalFragment extends BaseIdentityFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_identity_final, container, false);
    }

    @Override
    public String check() {
        return null;
    }
}
