package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.ChatMessageAdapter;
import de.qabel.qabelbox.adapter.ChatMessageAdapter.OnItemClickListener;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatMessageItem.ShareMessagePayload;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.chat.ChatServer.ChatServerCallback;
import de.qabel.qabelbox.chat.ShareHelper;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.services.LocalQabelService.OnSendDropMessageResult;
import de.qabel.qabelbox.storage.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class ContactChatFragment extends ContactBaseFragment {
    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = getClass().getSimpleName();

    private Contact contact;
    private final ArrayList<ChatMessageItem> messages = new ArrayList<>();


    private ListView contactListRecyclerView;
    private View emptyView;
    private EditText etText;
    private ChatServer chatServer;
    private boolean isSyncing;

    public static ContactChatFragment newInstance(Contact contact) {
        ContactChatFragment fragment = new ContactChatFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, contact);
        fragment.setArguments(args);
        fragment.contact = contact;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatServer = mActivity.chatServer;

        setHasOptionsMenu(true);
        //@todo reactivate this later. serialize contact ignore dropurls
        /*Bundle arguments = getArguments();
        if (arguments != null) {
			contact = (Contact) arguments.getSerializable(ARG_IDENTITY);
		}
		else
		{
			new Throwable("No contact given");
		}*/
        ((MainActivity) getActivity()).toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(layout.fragment_contact_chat, container, false);
        contactListRecyclerView = (ListView) view.findViewById(id.contact_chat_list);
        emptyView = view.findViewById(id.empty_view);
        etText = (EditText) view.findViewById(id.etText);
        TextView send = (Button) view.findViewById(id.bt_send);
        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = etText.getText().toString();
                if (text.length() > 0) {
                    sendMessage(text);
                }
            }
        });
        etText.setText("");

        refreshMessages();
        refreshMessagesAsync();

        return view;
    }

    private void sendMessage(String text) {
        try {
            final DropMessage dropMessage = chatServer.getTextDropMessage(text);
            final Identity identity = QabelBoxApplication.getInstance().getService().getActiveIdentity();
            QabelBoxApplication.getInstance().getService().sendDropMessage(dropMessage, contact, identity, new OnSendDropMessageResult() {
                @Override
                public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {
                    boolean sended = false;
                    Log.v(TAG, "delivery status: " + deliveryStatus);
                    if (deliveryStatus != null) {
                        Iterator it = deliveryStatus.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry pair = (Entry) it.next();
                            if ((Boolean) pair.getValue()) {
                                sended = true;
                            }
                            Log.d(TAG, "message send result: " + pair + " " + pair.getValue());
                        }

                        Log.d(TAG, "sended: " + sended);
                        if (sended) {
                            ChatMessageItem newMessage = new ChatMessageItem(identity, contact.getEcPublicKey().getReadableKeyIdentifier(), dropMessage.getDropPayload(), dropMessage.getDropPayloadType());

                            chatServer.storeIntoDB(newMessage);
                            messages.add(newMessage);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    etText.setText("");
                                    fillAdapter(messages);
                                }
                            });
                        }
                    }
                    if (!sended) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), string.message_chat_message_not_sended, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                }
            });
        } catch (QblDropPayloadSizeException e) {
            Toast.makeText(getActivity(), string.cant_send_message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "cant send message", e);
        }
    }


    private void refreshMessagesAsync() {
        if (!isSyncing) {
            isSyncing = true;
            new AsyncTask<Void, Void, Collection<DropMessage>>() {
                @Override
                protected void onPostExecute(Collection<DropMessage> dropMessages) {
                    refreshMessages();
                    isSyncing = false;
                }

                @Override
                protected Collection<DropMessage> doInBackground(Void... params) {
                    isSyncing = true;
                    return chatServer.refreshList();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Get the messages from the ChatServer and refreshes the local list and view.
     */
    private void refreshMessages() {
        ChatMessageItem[] items = chatServer.getAllMessages(contact);
        messages.clear();
        for (ChatMessageItem item : items) {
            Log.v(TAG, "add message " + item.drop_payload);
            messages.add(item);
        }
        chatServer.setAllMessagesReaded(contact);
        fillAdapter(messages);
    }


    private void fillAdapter(final ArrayList<ChatMessageItem> data) {
        ChatMessageAdapter contactListAdapter = new ChatMessageAdapter(data, contact);
        contactListAdapter.setEmptyView(emptyView);
        contactListRecyclerView.setAdapter(contactListAdapter);
        contactListAdapter.setOnItemClickListener(getOnItemClickListener());
        contactListAdapter.notifyDataSetChanged();
    }

    @NonNull
    private OnItemClickListener getOnItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public void onItemClick(final ChatMessageItem item) {
                LocalQabelService service = QabelBoxApplication.getInstance().getService();

                //check if message is instance of sharemessage
                if (item.getData() instanceof ShareMessagePayload) {
                    final FilesFragment filesFragment = mActivity.filesFragment;

                    //check if share from other (not my sended share)
                    if (!item.getSenderKey().equals(service.getActiveIdentity().getEcPublicKey().getReadableKeyIdentifier())) {
                        new AsyncTask<Void, Void, BoxNavigation>() {
                            int errorId;
                            public AlertDialog wait;

                            @Override
                            protected void onPreExecute() {
                                wait = UIHelper.showWaitMessage(getActivity(), string.infos, string.message_please_wait, false);
                            }

                            @Override
                            protected BoxNavigation doInBackground(Void... params) {
                                //navigate to share folder or create this if not exists
                                BoxNavigation nav = navigateToShareFolder(filesFragment.getBoxVolume());
                                if (nav == null) {
                                    errorId = string.message_cant_navigate_to_share_folder;
                                    return null;
                                }
                                //check if shared file already exists or attached
                                if (isAttached(nav, item)) {
                                    errorId = string.message_cant_attach_external_file_exists;
                                    return null;
                                }
                                return nav;
                            }

                            @Override
                            protected void onPostExecute(BoxNavigation boxNavigation) {
                                wait.dismiss();
                                if (boxNavigation == null) {
                                    UIHelper.showDialogMessage(getActivity(), string.dialog_headline_info, errorId);
                                } else {
                                    BoxExternalReference boxExternalReference = ShareHelper.getBoxExternalReference(contact, item);
                                    attachCheckedSharedFile(filesFragment, boxNavigation, boxExternalReference);
                                }
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        };
    }

    /**
     * attach a file to boxvolume. Call this after all checks done
     */
    private void attachCheckedSharedFile(final FilesFragment filesFragment, final BoxNavigation nav, final BoxExternalReference boxExternalReference) {
        new AsyncTask<Void, Void, List<BoxObject>>() {
            AlertDialog wait;

            @Override
            protected void onPostExecute(List<BoxObject> boxObjects) {
                if (boxObjects != null) {
                    Toast.makeText(mActivity, string.shared_file_imported, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, string.cant_import_shared_file, Toast.LENGTH_SHORT).show();
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
                wait = UIHelper.showWaitMessage(mActivity, string.dialog_headline_info, string.please_wait_attach_external_file, false);
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * navigate to share folder. if folder don't exists, create it.
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
     */
    private boolean isAttached(BoxNavigation nav, ChatMessageItem item) {
        ShareMessagePayload payLoad = (ShareMessagePayload) item.getData();
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
        inflater.inflate(menu.ab_chat_detail_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == id.action_chat_detail_refresh) {
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

    private final ChatServerCallback chatServerCallback = new ChatServerCallback() {
        @Override
        public void onRefreshed() {
        }
    };
}
