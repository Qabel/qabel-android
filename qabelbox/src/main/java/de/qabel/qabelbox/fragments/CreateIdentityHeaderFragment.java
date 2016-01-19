package de.qabel.qabelbox.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentityHeaderFragment extends Fragment {

    private TextView tvDescription;
    private ViewGroup logoLayout;
    private ViewGroup initialLayout;
    private TextView tvInitial;
    private TextView tvName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_header, container, false);
        logoLayout = (ViewGroup) view.findViewById(R.id.logo_layout);
        initialLayout = (ViewGroup) view.findViewById(R.id.initial_layout);
        tvInitial = (TextView) view.findViewById(R.id.tv_initial);
        tvName = (TextView) view.findViewById(R.id.tv_name);
        tvDescription = (TextView) view.findViewById(R.id.tv_description);
        updateUI(null, null, Integer.MIN_VALUE);
        return view;
    }

    public void updateUI(String name, String description, int securityLevel) {

        if (name == null) {
            logoLayout.setVisibility(View.VISIBLE);
            initialLayout.setVisibility(View.INVISIBLE);
            return;
        }
        logoLayout.setVisibility(View.INVISIBLE);
        initialLayout.setVisibility(View.VISIBLE);
        tvName.setText(name);
        tvInitial.setText(getInitials(name));
        tvDescription.setText(description);
    }

    private String getInitials(String name) {

        String[] names = name.split(" ");
        String result = "";
        for (String value : names) {
            if (result.length() < 2) {
                result += value.toUpperCase().charAt(0);
            }
        }
        System.out.println("ini '"+result+"'");
        return result;
    }
}
