package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 23.02.16.
 */
public class WelcomeDisclaimerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome_disclaimer, container, false);
        setShader((TextView) view.findViewById(R.id.welcome_text1));
        setShader((TextView) view.findViewById(R.id.welcome_text2));
        setShader((TextView) view.findViewById(R.id.welcome_text3));
        setShader((TextView) view.findViewById(R.id.welcome_text4));

        setSmallShader((TextView) view.findViewById(R.id.cb_welcome_legal));
        setSmallShader((TextView) view.findViewById(R.id.cb_welcome_privacy));
        return view;
    }

    private void setShader(TextView tv) {
        float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
        float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
        float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
        int col = getResources().getColor(R.color.welcome_shadow);
        tv.setShadowLayer(radius, dx, dy, col);
    }
    private void setSmallShader(TextView tv) {
        float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
        float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
        float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
        int col = getResources().getColor(R.color.welcome_shadow);
        tv.setShadowLayer(radius, dx, dy, col);
    }
}
