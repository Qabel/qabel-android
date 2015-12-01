package de.qabel.qabelbox.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.R;

public class AddIdentityFragment extends Fragment {

    private EditText textViewIdentityName;
    private EditText textViewDropServer;
    private Button buttonAddIdentity;
    private AddIdentityListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_add_identity, container, false);
        textViewIdentityName = (EditText) view.findViewById(R.id.editTextIdentityAlias);
        textViewDropServer = (EditText) view.findViewById(R.id.editTextIdentityDropServer);
        buttonAddIdentity = (Button) view.findViewById(R.id.buttonAddIdentity);

        buttonAddIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URI uri = URI.create(textViewDropServer.getText().toString());
                DropServer dropServer = new DropServer(uri, "", true);
                DropURL dropURL = new DropURL(dropServer);
                Collection<DropURL> dropURLs = new ArrayList<>();
                dropURLs.add(dropURL);

                Identity identity = new Identity(textViewIdentityName.getText().toString(),
                        dropURLs, new QblECKeyPair());

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                mListener.addIdentity(identity);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (AddIdentityListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddIdentityListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface AddIdentityListener {
        void addIdentity(Identity identity);
    }
}
