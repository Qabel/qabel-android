package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.qabel.core.storage.BoxNavigation;
import de.qabel.qabelbox.R;

public class NewFolderFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private EditText editTextNewFolder;
    private Button buttonAddFolder;
    private BoxNavigation boxNavigation;

    public NewFolderFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_new_folder, container, false);

        buttonAddFolder = (Button) view.findViewById(R.id.buttonAdd);
        editTextNewFolder = (EditText) view.findViewById(R.id.editTextNewFolderName);

        buttonAddFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCreateFolder(editTextNewFolder.getText().toString(), boxNavigation);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setBoxNavigation(BoxNavigation boxNavigation) {
        this.boxNavigation = boxNavigation;
    }

    public interface OnFragmentInteractionListener {
        void onCreateFolder(String name, BoxNavigation boxNavigation);
    }

}
