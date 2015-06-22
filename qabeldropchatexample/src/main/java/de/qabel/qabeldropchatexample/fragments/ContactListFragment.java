package de.qabel.qabeldropchatexample.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.qabel.IContact;
import de.qabel.qabeldropchatexample.R;
import de.qabel.qabeldropchatexample.adapter.ContactsAdapter;

public class ContactListFragment extends Fragment implements RecyclerView.OnClickListener {

    private static final String ARG_CONTACT = "ARG_CONTACT";

    private OnContactSelectedListener mListener;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public static ContactListFragment newInstance(ArrayList<IContact> contacts) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT, contacts);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<IContact> contacts = null;
        if (getArguments() != null) {
            contacts = (ArrayList<IContact>) getArguments().getSerializable(ARG_CONTACT);
        }

        mAdapter = new ContactsAdapter(contacts, new ContactsAdapter.OnContactSelectedListener() {
            @Override
            public void onContactSelected(IContact contact) {
                mListener.onContactSelected(contact);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contactlist_list, container, false);

        mLayoutManager = new LinearLayoutManager(getActivity());

        mRecyclerView = (RecyclerView) view.findViewById(R.id.contacts_lists);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mRecyclerView.setOnClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnContactSelectedListener) activity;
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

    @Override
    public void onClick(View view) {
    }

    public interface OnContactSelectedListener {
        void onContactSelected(IContact contact);
    }

}
