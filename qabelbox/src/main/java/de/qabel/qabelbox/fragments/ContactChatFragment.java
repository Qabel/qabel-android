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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.ChatMessageAdapter;
import de.qabel.qabelbox.chat.ChatServer;
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
    private TextView send;
    private EditText etText;
    private ChatServer chatServer;

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
        chatServer = ChatServer.getInstance();
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
        etText = (EditText) view.findViewById(R.id.etText);
        send = (Button) view.findViewById(R.id.bt_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getActivity(), "send text " + contact.getEcPublicKey().getReadableKeyIdentifier() + " " + etText.getText().toString(), Toast.LENGTH_LONG).show();
                String[]temp = contact.getDropUrls().iterator().next().toString().split("/");
                Identity currentIdentity=QabelBoxApplication.getInstance().getService().getActiveIdentity();

                long id = chatServer.getNextId();
                chatServer.sendTextMessage(id,temp[temp.length-1], etText.getText().toString(), currentIdentity,contact.getEcPublicKey().getReadableKeyIdentifier().toString());
            }
        });
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
            item.drop_payload = json.toString();
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
        inflater.inflate(R.menu.ab_chat_detail_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_chat_detail_refresh) {
            long mId = chatServer.getNextId();
            chatServer.refreshList(mId, QabelBoxApplication.getInstance().getService().getActiveIdentity());
        }

        return super.onOptionsItemSelected(item);
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

    @Override
    public void onStart() {

        super.onStart();
        chatServer.addListner(chatServerCallback);
    }

    @Override
    public void onStop() {

        chatServer.removeListner(chatServerCallback);
        super.onStop();
    }

    private ChatServer.ChatServerCallback chatServerCallback = new ChatServer.ChatServerCallback() {

        @Override
        public void onSuccess(long id) {

        }

        @Override
        public void onError(long id) {

        }
    };
}
