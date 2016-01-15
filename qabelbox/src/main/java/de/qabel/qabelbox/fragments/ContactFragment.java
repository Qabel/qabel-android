package de.qabel.qabelbox.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.ContactsAdapter;

/**
 * Fragment that shows a contact list.
 */
public class ContactFragment extends BaseFragment {

    private static final String ARG_CONTACTS = "ARG_CONTACTS";
    private static final String ARG_IDENTITY = "ARG_IDENTITY";

    private RecyclerView contactListRecyclerView;
    private ContactsAdapter contactListAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;

    private Contacts contacts;
    private Identity identity;
    private ContactListListener mListener;

    public static ContactFragment newInstance(Contacts contacts, Identity identity) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACTS, contacts);
        args.putSerializable(ARG_IDENTITY, identity);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            contacts = (Contacts) arguments.getSerializable(ARG_CONTACTS);
            identity = (Identity) arguments.getSerializable(ARG_IDENTITY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        contactListRecyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        contactListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        contactListAdapter = new ContactsAdapter(contacts);
        contactListRecyclerView.setAdapter(contactListAdapter);

        contactListAdapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getActivity().getApplicationContext(), "Selected " + position, Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ContactListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ContactListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface ContactListListener {
        void startAddContact(Identity identity);
    }
    @Override
    public String getTitle() {
        return getString(R.string.headline_contacts);
    }
    @Override
    public boolean isFabNeeded() {
        return true;
    }

     public boolean supportBackButton() {
        return false;
    }
}
