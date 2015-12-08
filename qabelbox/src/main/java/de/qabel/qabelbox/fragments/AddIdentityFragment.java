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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;

public class AddIdentityFragment extends Fragment {

    private static final int DEFAULT_DROP_BITS = 8;
    private EditText textViewIdentityName;
    private Button buttonAddIdentity;
    private Button buttonCancel;
    private CheckBox checkBoxAdvancedOptions;
    private TextView textViewDescriptionDropID;
    private TextView textViewAdvancedDescriptionDropID;
    private TextView textViewDropIDMin;
    private TextView textViewDropIDMax;
    private TextView textViewDropIDCurrent;
    private SeekBar seekBarDropID;
    private AddIdentityListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_add_identity, container, false);
        textViewIdentityName = (EditText) view.findViewById(R.id.editTextIdentityAlias);
        buttonAddIdentity = (Button) view.findViewById(R.id.buttonAddIdentity);
        buttonCancel = (Button) view.findViewById(R.id.buttonAddIdentityCancel);
        checkBoxAdvancedOptions = (CheckBox) view.findViewById(R.id.checkBoxAddIdentityAdvanced);
        textViewDescriptionDropID = (TextView) view.findViewById(R.id.textViewAddIdentityDescriptionDropID);
        textViewAdvancedDescriptionDropID = (TextView) view.findViewById(R.id.textViewAddIdentityDropIDAdvancedDescription);
        seekBarDropID = (SeekBar) view.findViewById(R.id.seekBarAddIdentityDropID);
        textViewDropIDMin = (TextView) view.findViewById(R.id.textViewAddIdentityDropIDMinBits);
        textViewDropIDMax = (TextView) view.findViewById(R.id.textViewAddIdentityDropIDMaxBits);
        textViewDropIDCurrent = (TextView) view.findViewById(R.id.textViewAddIdentityDropIDCurrentBits);

        textViewDropIDMin.setText(String.valueOf(0));
        textViewDropIDCurrent.setText(String.valueOf(DEFAULT_DROP_BITS));
        textViewDropIDMax.setText(String.valueOf(DropIdGenerator.DROP_ID_LENGTH_BYTE));

        seekBarDropID.setProgress(DEFAULT_DROP_BITS - 1);

        buttonAddIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
                DropServer dropServer = new DropServer(uri, "", true);
                DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(seekBarDropID.getProgress() + 1);
                DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
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

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelAddIdentity();
            }
        });

        seekBarDropID.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewDropIDCurrent.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        checkBoxAdvancedOptions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    textViewDescriptionDropID.setVisibility(View.VISIBLE);
                    textViewAdvancedDescriptionDropID.setVisibility(View.VISIBLE);
                    seekBarDropID.setVisibility(View.VISIBLE);
                    textViewDropIDMin.setVisibility(View.VISIBLE);
                    textViewDropIDMax.setVisibility(View.VISIBLE);
                    textViewDropIDCurrent.setVisibility(View.VISIBLE);
                } else {
                    textViewDescriptionDropID.setVisibility(View.INVISIBLE);
                    textViewAdvancedDescriptionDropID.setVisibility(View.INVISIBLE);
                    seekBarDropID.setVisibility(View.INVISIBLE);
                    textViewDropIDMin.setVisibility(View.INVISIBLE);
                    textViewDropIDMax.setVisibility(View.INVISIBLE);
                    textViewDropIDCurrent.setVisibility(View.INVISIBLE);
                    seekBarDropID.setProgress(DEFAULT_DROP_BITS - 1);
                }
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
        void cancelAddIdentity();
    }
}
