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

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalReference;
import de.qabel.qabelbox.storage.BoxObject;

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
                        final DropMessage dropMessage = chatServer.getTextDropMessage(text);
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
                                ChatMessageItem newMessage = chatServer.createOwnMessage(identity, contact.getEcPublicKey().getReadableKeyIdentifier().toString(), dropMessage.getDropPayload(), dropMessage.getDropPayloadType());
                                chatServer.storeIntoDB(newMessage);
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

    private void fillAdapter(final ArrayList<ChatMessageItem> data) {

        contactListAdapter = new ChatMessageAdapter(data, contact);
        contactListAdapter.setEmptyView(emptyView);
        contactListRecyclerView.setAdapter(contactListAdapter);

        contactListAdapter.setOnItemClickListener(new ChatMessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatMessageItem item) {

                if (item.getData() instanceof ChatMessageItem.ShareMessagePayload) {
                    ChatMessageItem.ShareMessagePayload payload = (ChatMessageItem.ShareMessagePayload) item.getData();
                    final BoxExternalReference boxExternalReference = new BoxExternalReference(false, payload.getURL(), payload.getMessage(), contact.getEcPublicKey(), Hex.decode(payload.getKey()));
                    final FilesFragment filesFragment = mActivity.filesFragment;
                    new AsyncTask<Void, Void, List<BoxObject>>() {
                        @Override
                        protected void onPostExecute(List<BoxObject> boxObjects) {

                            if (boxObjects != null) {
                                Toast.makeText(mActivity, R.string.shared_file_imported, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mActivity, R.string.cant_import_shared_file, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        protected List<BoxObject> doInBackground(Void... params) {

                            try {
                                mActivity.filesFragment.getBoxNavigation().attachExternal(boxExternalReference);
                                mActivity.filesFragment.getBoxNavigation().commit();
                                List<BoxObject> boxExternalFiles = null;

                                boxExternalFiles = filesFragment.getBoxNavigation().listExternals();
                                for (BoxObject extFile : boxExternalFiles) {
                                    Log.v(TAG, "external files " + extFile.name);
                                }
                                return boxExternalFiles;
                            } catch (QblStorageException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                }
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
            message.receiver = null;
            message.time_stamp = item.getCreationDate().getTime();
            message.acknowledge_id = item.getAcknowledgeID();
            message.drop_payload = item.getDropPayload();
            message.drop_payload_type = item.getDropPayloadType();
            message.isNew = 1;
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
