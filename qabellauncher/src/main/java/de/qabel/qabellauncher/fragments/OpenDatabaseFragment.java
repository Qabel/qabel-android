package de.qabel.qabellauncher.fragments;

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

import de.qabel.qabellauncher.R;


/**
 * Fragment that asks for the database encryption password. Password entered on first
 * application launch is used for database encryption.
 */
public class OpenDatabaseFragment extends Fragment {

    private OpenDatabaseFragmentListener mListener;
    private Button btnOpen;
    private EditText editTextPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_open_database, container, false);

        btnOpen = (Button) rootView.findViewById(R.id.buttonOpen);
        editTextPassword = (EditText) rootView.findViewById(R.id.editTextPassword);

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextPassword.length() == 0) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.enter_db_password, Toast.LENGTH_LONG).show();
                    return;
                }

                char[] password = new char[editTextPassword.length()];
                editTextPassword.getText().getChars(0, editTextPassword.length(), password, 0);

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                mListener.onPasswordEntered(password);
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OpenDatabaseFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OpenDatabaseFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OpenDatabaseFragmentListener {
        void onPasswordEntered(char[] password);
    }
}
