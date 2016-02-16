package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.qabel.core.config.Contact;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.ChatMessageAdapter;
import de.qabel.qabelbox.communication.model.ChatMessageItem;

/**
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class ContactChatFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = this.getClass().getSimpleName();

    private Contact contact;

    private View mView;
    private ContactChatFragment fragment;
    private RecyclerView contactListRecyclerView;
    private View emptyView;
    private LinearLayoutManager recyclerViewLayoutManager;
    private ChatMessageAdapter contactListAdapter;

    public static ContactChatFragment newInstance(Contact contact) {

        ContactChatFragment fragment = new ContactChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments != null) {
            contact = (Contact) arguments.getSerializable(ARG_IDENTITY);
        }
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();

        fragment = this;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_contact_chat, container, false);
        contactListRecyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        contactListRecyclerView.setHasFixedSize(true);
        emptyView = view.findViewById(R.id.empty_view);
        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        refreshContactList();
        actionBar.setSubtitle(contact.getAlias());
        mView = view;
        return view;
    }
    /**
     * refresh ui
     */
    private void refreshContactList() {

        ArrayList<ChatMessageItem> contacts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ChatMessageItem item = new ChatMessageItem();
            item.time_stamp = System.currentTimeMillis();
            item.sender = "sendkey";
            JSONObject json = new JSONObject();
            try {
                json.put("message", "meine nachricht");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            item.drop_payload = json;
            contacts.add(item);
        }
        if (contactListRecyclerView != null) {
            int count = contacts.size();

            contactListAdapter = new ChatMessageAdapter(contacts, contact);
            contactListAdapter.setEmptyView(emptyView);
            contactListRecyclerView.setAdapter(contactListAdapter);
            contactListAdapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshList();
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshList() {

    }

    @Override
    public boolean isFabNeeded() {

        return false;
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_contact_chat);
    }

    @Override
    public boolean supportBackButton() {

        return true;
    }
}
