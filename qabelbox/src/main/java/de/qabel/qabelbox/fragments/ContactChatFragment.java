package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.ChatMessageAdapter;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.chat.ShareHelper;
import de.qabel.qabelbox.dagger.components.MainActivityComponent;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.storage.BoxExternalReference;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

/**
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class ContactChatFragment extends ContactBaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = this.getClass().getSimpleName();

    private Contact contact;
    private final ArrayList<ChatMessageItem> messages = new ArrayList<>();


    @BindView(R.id.contact_chat_list)
    ListView contactListRecyclerView;

    @BindView(R.id.etText)
    EditText etText;

    @Inject
    ChatServer chatServer;

    @Inject
    Identity activeIdentity;

    @Inject
    DropConnector dropConnector;

    @Inject
    Context context;

    public static ContactChatFragment newInstance(Contact contact) {

        ContactChatFragment fragment = new ContactChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, contact);
        fragment.setArguments(args);
        fragment.contact = contact;
        return fragment;
    }

    private DropConnector getDropConnector() {
        return dropConnector;
    }

    private Identity getIdentity() {
        return activeIdentity;
    }

    public Contact getContact() {
        return contact;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getComponent(MainActivityComponent.class).inject(this);

        setHasOptionsMenu(true);
        ((MainActivity) getActivity()).toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
        mActivity.registerReceiver(refreshChatIntentReceiver, new IntentFilter(Helper.INTENT_REFRESH_CHAT));
        refreshMessages();
        refreshMessagesAsync();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity.unregisterReceiver(refreshChatIntentReceiver);
    }

    private final BroadcastReceiver refreshChatIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "receive refresh chat event");
            refreshMessages();
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_contact_chat, container, false);
        ButterKnife.bind(this, view);
        etText.setText("");

        return view;
    }

    @OnClick(R.id.bt_send)
    void clickSend(TextView send) {
        send.setOnClickListener(v -> {
            final String text = etText.getText().toString();
            if (text.length() > 0) {
                sendMessage(text);
            }
        });
    }

    private void sendMessage(String text) {
        try {
            final DropMessage dropMessage = ChatServer.createTextDropMessage(getIdentity(), text);
            final Identity identity = getIdentity();
            getDropConnector().sendDropMessage(dropMessage, contact, identity, deliveryStatus -> {
                boolean sent = false;
                Log.v(TAG, "delivery status: " + deliveryStatus);
                if (deliveryStatus != null) {
                    for (Object o : deliveryStatus.entrySet()) {
                        Map.Entry pair = (Map.Entry) o;
                        if ((Boolean) pair.getValue()) {
                            sent = true;
                        }
                        Log.d(TAG, "message send result: " + pair.toString() + " " + pair.getValue());
                    }

                    Log.d(TAG, "sent: " + sent);
                    if (sent) {
                        ChatMessageItem newMessage = new ChatMessageItem(identity, contact.getEcPublicKey().getReadableKeyIdentifier(), dropMessage.getDropPayload(), dropMessage.getDropPayloadType());

                        chatServer.storeIntoDB(getIdentity(), newMessage);
                        messages.add(newMessage);

                        getActivity().runOnUiThread(() -> {
                            etText.setText("");
                            fillAdapter(messages);
                        });
                    }
                }
                if (!sent) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.message_chat_message_not_sent, Toast.LENGTH_SHORT).show());

                }

            });
        } catch (QblDropPayloadSizeException e) {
            Toast.makeText(getActivity(), R.string.cant_send_message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "cant send message", e);
        }
    }

    private void refreshMessagesAsync() {
        AccountHelper.startOnDemandSyncAdapter();
    }

    /**
     * Get the messages from the ChatServer and refreshes the local list and view.
     */
    private void refreshMessages() {
        ChatMessageItem[] items = chatServer.getAllMessages(getIdentity(), contact);
        messages.clear();
        for (ChatMessageItem item : items) {
            Log.v(TAG, "add message " + item.drop_payload);
            messages.add(item);
        }
        chatServer.setAllMessagesRead(getIdentity(), contact);
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent);
        fillAdapter(messages);
    }


    private void fillAdapter(final ArrayList<ChatMessageItem> data) {

        if (contactListRecyclerView.getAdapter() == null) {
            ChatMessageAdapter contactListAdapter = new ChatMessageAdapter(data, contact);
            contactListRecyclerView.setAdapter(contactListAdapter);
            contactListAdapter.setOnItemClickListener(getOnItemClickListener());
        }
        ChatMessageAdapter adapter = (ChatMessageAdapter) contactListRecyclerView.getAdapter();
        adapter.setMessages(data, contact);
        adapter.notifyDataSetChanged();
    }

    @NonNull
    private ChatMessageAdapter.OnItemClickListener getOnItemClickListener() {

        return item -> {
            //check if message is instance of sharemessage
            if (item.getData() instanceof ChatMessageItem.ShareMessagePayload) {

                final FilesFragment filesFragment = mActivity.filesFragment;

                //check if share from other (not my sended share)
                String keyIdentifier = mActivity.getActiveIdentity()
                        .getEcPublicKey().getReadableKeyIdentifier();
                if (!item.getSenderKey().equals(keyIdentifier)) {

                    new AsyncTask<Void, Void, BoxNavigation>() {
                        int errorId;
                        public AlertDialog wait;

                        @Override
                        protected void onPreExecute() {
                            wait = UIHelper.showWaitMessage(getActivity(), R.string.infos, R.string.message_please_wait, false);
                        }

                        @Override
                        protected BoxNavigation doInBackground(Void... params) {

                            //navigate to share folder or create this if not exists
                            BoxNavigation nav = navigateToShareFolder(filesFragment.getBoxVolume());
                            if (nav == null) {
                                errorId = R.string.message_cant_navigate_to_share_folder;
                                return null;
                            }
                            //check if shared file already exists or attached
                            if (isAttached(nav, item)) {
                                errorId = R.string.message_cant_attach_external_file_exists;
                                return null;
                            }
                            return nav;
                        }

                        @Override
                        protected void onPostExecute(BoxNavigation boxNavigation) {

                            wait.dismiss();
                            if (boxNavigation == null) {
                                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, errorId);
                            } else {
                                BoxExternalReference boxExternalReference = ShareHelper.getBoxExternalReference(contact, item);
                                attachCheckedSharedFile(filesFragment, boxNavigation, boxExternalReference);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        };
    }

    /**
     * attach a file to boxvolume. Call this after all checks done
     *
     * @param filesFragment
     * @param nav
     * @param boxExternalReference
     */
    private void attachCheckedSharedFile(final FilesFragment filesFragment, final BoxNavigation nav, final BoxExternalReference boxExternalReference) {

        new AsyncTask<Void, Void, List<BoxObject>>() {
            AlertDialog wait;

            @Override
            protected void onPostExecute(List<BoxObject> boxObjects) {

                if (boxObjects != null) {
                    Toast.makeText(mActivity, R.string.shared_file_imported, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, R.string.cant_import_shared_file, Toast.LENGTH_SHORT).show();
                }
                filesFragment.refresh();
                //	filesFragment.setBoxNavigation(nav);
                wait.dismiss();
            }

            @Override
            protected List<BoxObject> doInBackground(Void... params) {

                try {
                    nav.attachExternal(boxExternalReference);
                    nav.commit();
                    List<BoxObject> boxExternalFiles;
                    boxExternalFiles = nav.listExternals();
                    return boxExternalFiles;
                } catch (QblStorageException e) {
                    Log.e(TAG, "can't attach shared file", e);
                }
                return null;
            }

            @Override
            protected void onPreExecute() {

                wait = UIHelper.showWaitMessage(mActivity, R.string.dialog_headline_info, R.string.please_wait_attach_external_file, false);
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * navigate to share folder. if folder don't exists, create it.
     *
     * @param boxVolume
     * @return
     */
    private BoxNavigation navigateToShareFolder(BoxVolume boxVolume) {

        try {

            BoxNavigation nav = boxVolume.navigate();
            List<BoxFolder> folders = nav.listFolders();

            for (BoxFolder folder : folders) {

                if (folder.name.equals(BoxFolder.RECEIVED_SHARE_NAME)) {
                    nav.navigate(folder);
                    return nav;
                }
            }
            BoxFolder folder = nav.createFolder(BoxFolder.RECEIVED_SHARE_NAME);
            nav.commit();
            if (folder != null) {
                nav.navigate(folder);
                return nav;
            } else {
                return null;
            }
        } catch (QblStorageException e) {
            Log.e(TAG, "error on navigate to share folder", e);
        }
        return null;
    }

    /**
     * check if given item already attached or exists in the share folder
     *
     * @param nav
     * @param item
     * @return
     */
    private boolean isAttached(BoxNavigation nav, ChatMessageItem item) {

        ChatMessageItem.ShareMessagePayload payLoad = (ChatMessageItem.ShareMessagePayload) item.getData();
        String fileNameToAdd = payLoad.getMessage();
        try {
            //go through external files
            List<BoxObject> external = nav.listExternals();
            for (BoxObject externalBoxObject : external) {
                if (externalBoxObject.name.equals(fileNameToAdd)) {
                    return true;
                }
            }
            //go through files
            List<BoxFile> files = nav.listFiles();
            for (BoxObject boxOject : files) {
                if (boxOject.name.equals(fileNameToAdd)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on parse share folder", e);
            return false;
        }
        return false;
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
        return contact.getAlias();
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

    private final ChatServer.ChatServerCallback chatServerCallback = new ChatServer.ChatServerCallback() {

        @Override
        public void onRefreshed() {

        }
    };
}
