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
import android.widget.Toast;

import de.qabel.qabelbox.R;


/**
 * Activities that contain this fragment must implement the
 * {@link NewDatabasePasswordListener} interface
 * to handle interaction events.
 */
public class NewDatabasePasswordFragment extends Fragment {

    private NewDatabasePasswordListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView =  inflater.inflate(R.layout.fragment_new_database_password, container, false);

        final EditText editTextPassword = (EditText) rootView.findViewById(R.id.editTextPassword);
        final EditText editTextPasswordConfirm = (EditText) rootView.findViewById(R.id.editTextPasswordConfirm);

        Button btnNewDbPassword = (Button) rootView.findViewById(R.id.buttonEnterNewDbPassword);
        btnNewDbPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextPassword.getText().length() == 0) {
                    Toast.makeText(getActivity(), R.string.password_cant_be_empty, Toast.LENGTH_LONG).show();
                    return;
                }
                if (editTextPassword.getText().toString().equals(editTextPasswordConfirm.getText().toString())) {
                    char[] password = new char[editTextPassword.length()];
                    editTextPassword.getText().getChars(0, editTextPassword.length(), password, 0);
                    mListener.onNewPasswordEntered(password);
                } else {
                    Toast.makeText(getActivity(), R.string.password_does_not_match, Toast.LENGTH_LONG).show();
                }

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (NewDatabasePasswordListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NewDatabasePasswordListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface NewDatabasePasswordListener {
        void onNewPasswordEntered(char[] newPassword);
    }
}
