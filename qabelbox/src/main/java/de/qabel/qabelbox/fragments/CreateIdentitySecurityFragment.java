package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentitySecurityFragment extends BaseIdentityFragment {

    private SeekBar sbSecurity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_security, container, false);
        sbSecurity= (SeekBar) view.findViewById(R.id.sb_security);
        sbSecurity.setMax(4);
        return view;
    }
    @Override
    public String check() {
        mActivty.setSecurity(sbSecurity.getProgress());
        return null;
    }
    @Override
    public void resetData() {

        if (sbSecurity != null)
            sbSecurity.setProgress(-1);
    }
    @Override
    public void onBackPressed() {

        super.onBackPressed();
        mActivty.setSecurity(Integer.MIN_VALUE);
    }
}
