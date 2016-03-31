package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.activities.BaseWizardActivity;
import de.qabel.qabelbox.activities.BaseWizardActivity.NextChecker;

public class CreateIdentityDropBitsFragment extends BaseIdentityFragment {

    private SeekBar sbSecurity;
    private NextChecker mChecker;

    public static CreateIdentityDropBitsFragment newInstance(NextChecker checker) {

        CreateIdentityDropBitsFragment fragment = new CreateIdentityDropBitsFragment();

        fragment.mChecker = checker;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(layout.fragment_create_identity_drop_bits, container, false);
        sbSecurity = (SeekBar) view.findViewById(id.sb_security);
        return view;
    }

    @Override
    public String check() {
        return mChecker.check(sbSecurity);
    }

}
