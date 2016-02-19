package de.qabel.qabelbox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.ChatMessageAdapter;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class ContactChatFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = this.getClass().getSimpleName();

    private Contact contact;
    ArrayList<ChatMessageItem> messages = new ArrayList<>();

    private View mView;
    private ContactChatFragment fragment;
    private RecyclerView contactListRecyclerView;
    private View emptyView;
    private LinearLayoutManager recyclerViewLayoutManager;
    private ChatMessageAdapter contactListAdapter;
    private TextView send;
    private EditText etText;
    private ChatServer chatServer;
    Hashtable<Long, JSONObject> messageMap = new Hashtable();
    private String contactPublicKey;
    private String identityPublicKey;

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
        contactPublicKey = contact.getEcPublicKey().getReadableKeyIdentifier().toString();
        identityPublicKey = QabelBoxApplication.getInstance().getService().getActiveIdentity().getEcPublicKey().toString();
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
        fragment = this;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_contact_chat, container, false);
        contactListRecyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        contactListRecyclerView.setHasFixedSize(true);
        emptyView = view.findViewById(R.id.empty_view);
        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        recyclerViewLayoutManager.setReverseLayout(true);
        contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        etText = (EditText) view.findViewById(R.id.etText);
        send = (Button) view.findViewById(R.id.bt_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String text = etText.getText().toString();
                if (text.length() > 0) {

                    try {
                        DropMessage dropMessage = chatServer.getTextDropMessage(text);
                        final Identity identity = QabelBoxApplication.getInstance().getService().getActiveIdentity();
                        QabelBoxApplication.getInstance().getService().sendDropMessage(dropMessage, contact, identity, new LocalQabelService.OnSendDropMessageResult() {
                            @Override
                            public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        etText.setText("");
                                    }
                                });
                                ChatMessageItem newMessage = chatServer.createOwnMessage(identity, contact.getEcPublicKey().getReadableKeyIdentifier().toString(), text);
                                chatServer.storeOwnInDb(newMessage);
                                messages.add(newMessage);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        fillAdapter(messages);
                                    }
                                });
                            }
                        });
                    } catch (QblDropPayloadSizeException e) {
                        Toast.makeText(getActivity(), R.string.cant_send_message, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "cant send message", e);
                    }
                }
                ;
            }
        });
        etText.setText("");

        refreshMessagesAsync();
        actionBar.setSubtitle(contact.getAlias());
        mView = view;
        long mId = chatServer.getNextId();
        refreshMessagesAsync();

        return view;
    }

    protected void refreshMessagesAsync() {

        new AsyncTask<Void, Void, Collection<DropMessage>>() {
            @Override
            protected void onPostExecute(Collection<DropMessage> dropMessages) {

                messages.clear();

                messages = convertDropMessageToDatabaseMessage(dropMessages);

                refreshContactList(dropMessages);
            }

            @Override
            protected Collection<DropMessage> doInBackground(Void... params) {

                return chatServer.refreshList();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ChatMessageItem makeDummyEntry(String text) {

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
        return item;
    }

    /**
     * refresh ui
     *
     * @param pMessages
     */
    private void refreshContactList(Collection<DropMessage> pMessages) {

        if (contactListRecyclerView != null) {
           messages = convertDropMessageToDatabaseMessage(pMessages);
            chatServer.addMessagesFromDataBase(messages);
            fillAdapter(messages);
        }
    }

    private void fillAdapter(ArrayList<ChatMessageItem> data) {

        contactListAdapter = new ChatMessageAdapter(data, contact);
        contactListAdapter.setEmptyView(emptyView);
        contactListRecyclerView.setAdapter(contactListAdapter);
        contactListRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//@todo dont work temp
                Toast.makeText(getActivity(), "tbd: import share", Toast.LENGTH_SHORT).show();
            }
        });
        contactListAdapter.notifyDataSetChanged();
    }

    @NonNull
    private ArrayList<ChatMessageItem> convertDropMessageToDatabaseMessage(Collection<DropMessage> messages) {

        ArrayList<ChatMessageItem> data = new ArrayList<>();
        for (DropMessage item : messages) {
            ChatMessageItem message = new ChatMessageItem();
            message.sender = item.getSenderKeyId();
            message.time_stamp = item.getCreationDate().getTime();
            message.drop_payload_type = item.getDropPayloadType();
            message.drop_payload = item.getDropPayload();
            message.receiver = null;
            message.isNew = 1;
            message.acknowledge_id = item.getAcknowledgeID();
            data.add(message);
        }
        return data;
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
            refreshMessagesAsync();
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
        chatServer.addListener(chatServerCallback);
    }

    @Override
    public void onStop() {

        chatServer.removeListener(chatServerCallback);
        super.onStop();
    }

    private ChatServer.ChatServerCallback chatServerCallback = new ChatServer.ChatServerCallback() {

        @Override
        public void onRefreshed() {

        }
    };
}
