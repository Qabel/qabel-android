package de.qabel.qabelbox.fragments;

import android.graphics.Color;
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
public class WelcomeTextFragment extends Fragment {

    private static final String KEY_MESSAGE_ID = "messageid";


    public static Fragment newInstance(int id) {
        WelcomeTextFragment fragment = new WelcomeTextFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_MESSAGE_ID, id);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_welcome_text, container, false);
        TextView tv = ((TextView) view.findViewById(R.id.welcome_message));
        tv.setText(getArguments().getInt(KEY_MESSAGE_ID));

        setShader(tv);

        return view;
    }

    private void setShader(TextView tv) {
        float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
        float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
        float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
        int col = getResources().getColor(R.color.welcome_shadow);
        tv.setShadowLayer(radius, dx, dy, col);
    }
}
