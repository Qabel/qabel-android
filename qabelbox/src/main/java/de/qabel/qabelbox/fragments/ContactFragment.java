package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cocosw.bottomsheet.BottomSheet;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.spongycastle.util.encoders.Hex;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.ContactsAdapter;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.communication.DropServer;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Fragment that shows a contact list.
 */
public class ContactFragment extends BaseFragment {

    private static final String ARG_CONTACTS = "ARG_CONTACTS";
    private static final String ARG_IDENTITY = "ARG_IDENTITY";
    private static final int REQUEST_IMPORT_CONTACT = 1000;
    private static final String TAG = "ContactFragment";

    private RecyclerView contactListRecyclerView;
    private ContactsAdapter contactListAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;

    private Contacts contacts;
    private Identity identity;
    private BaseFragment self;
    private TextView contactCount;
    private View emptyView;
    ChatServer chatServer;

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
        self = this;
        chatServer=ChatServer.getInstance();
        setHasOptionsMenu(true);
        mActivity.registerReceiver(refreshContactListReceiver, new IntentFilter(Helper.INTENT_REFRESH_CONTACTLIST));
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
        contactCount = (TextView) view.findViewById(R.id.contactCount);
        contactListRecyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
        contactListRecyclerView.setHasFixedSize(true);
        emptyView = view.findViewById(R.id.empty_view);
        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        refreshContactList(contacts);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_chat_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_chat_refresh) {
            long mId=chatServer.getNextId();
            ChatServer.getInstance().refreshList(mId,QabelBoxApplication.getInstance().getService().getActiveIdentity());
        }

        return super.onOptionsItemSelected(item);
    }

    private void setClickListener() {

        contactListAdapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, final int position) {

                final Contact contact = contactListAdapter.getContact(position);
                UIHelper.showDialogMessage(getActivity(), getString(R.string.dialog_headline_warning),
                        getString(R.string.dialog_message_delete_contact_question).replace("%1", contact.getAlias()),
                        R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                LocalQabelService service = QabelBoxApplication.getInstance().getService();
                                service.deleteContact(contact);
                                refreshContactList(service.getContacts(service.getActiveIdentity()));
                                UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_info, getString(R.string.contact_deleted).replace("%1", contact.getAlias()));
                            }
                        }, null);
            }
        });

        contactListAdapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, final int position) {

                final Contact contact = contactListAdapter.getContact(position);
                getFragmentManager().beginTransaction().add(R.id.fragment_container, ContactChatFragment.newInstance(contact), MainActivity.TAG_CONTACT_CHAT_FRAGMENT).addToBackStack(MainActivity.TAG_CONTACT_CHAT_FRAGMENT).commit();
            }
        });
    }

    /**
     * add contact and show messages
     *
     * @param activity
     * @param contact
     */
    public static void addContact(MainActivity activity, Contact contact) {

        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        Iterator<Contact> iterator = service.getContacts(QabelBoxApplication.getInstance().getService().getActiveIdentity()).getContacts().iterator();
        boolean exits = false;
        while (iterator.hasNext()) {
            Contact con = iterator.next();
            if (con.getEcPublicKey().equals(contact.getEcPublicKey())) {
                exits = true;
                break;
            }
        }
        if (exits) {
            UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.cant_import_contact_already_exisits);
        } else {
            service.addContact(
                    contact);

            sendRefreshContactList(activity);
            UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.contact_import_successfull);
        }
    }

    private static void sendRefreshContactList(MainActivity activity) {

        Log.d(TAG, "send refresh intent");
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        activity.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {

        Log.v(TAG, "unregisterReceiver");
        mActivity.unregisterReceiver(refreshContactListReceiver);
        super.onDestroy();
    }

    private void refreshContactList(Contacts contacts) {

        if (contactListRecyclerView != null) {
            int count = contacts.getContacts().size();
            if (count == 0) {
                contactCount.setVisibility(View.INVISIBLE);
            } else {
                contactCount.setText(getString(R.string.contact_count).replace("%1", "" + count));
                contactCount.setVisibility(View.VISIBLE);
            }
            contactListAdapter = new ContactsAdapter(contacts);
            contactListAdapter.setEmptyView(emptyView);
            contactListRecyclerView.setAdapter(contactListAdapter);
            setClickListener();
            contactListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_contacts);
    }

    @Override
    public boolean isFabNeeded() {

        return true;
    }

    @Override
    public boolean handleFABAction() {

        new BottomSheet.Builder(mActivity).title(R.string.add_new_contact).sheet(R.menu.bottom_sheet_add_contact)
                .listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case R.id.add_contact_from_file:
                                addContactByFile();
                                break;
                            case R.id.add_contact_via_qr:
                                IntentIntegrator integrator = new IntentIntegrator(self);
                                integrator.initiateScan();
                                break;
                            case R.id.add_contact_direct_input:
                                selectAddContactFragment(QabelBoxApplication.getInstance().getService().getActiveIdentity());
                                break;
                        }
                    }
                }).show();

        return true;
    }

    private void addContactByFile() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_IMPORT_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMPORT_CONTACT) {
                if (resultData != null) {
                    Uri uri = resultData.getData();
                    try {
                        ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "r");
                        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                        String json = FileHelper.readFileAsText(fis);
                        fis.close();
                        Contact contact = ContactExportImport.parseContactForIdentity(QabelBoxApplication.getInstance().getService().getActiveIdentity(), json);
                        addContact(mActivity, contact);
                    } catch (Exception e) {
                        Log.w(TAG, "add contact failed", e);
                        UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, e);
                    }
                }
            }

            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, resultData);
            if (scanResult != null && scanResult.getContents() != null) {
                String[] result = scanResult.getContents().split("\\r?\\n");
                if (result.length == 4 && result[0].equals("QABELCONTACT")) {
                    try {
                        DropURL dropURL = new DropURL(result[2]);
                        Collection<DropURL> dropURLs = new ArrayList<>();
                        dropURLs.add(dropURL);

                        QblECPublicKey publicKey = new QblECPublicKey(Hex.decode(result[3]));
                        Contact contact = new Contact(result[1], dropURLs, publicKey);
                        ContactFragment.addContact(mActivity, contact);
                    } catch (Exception e) {
                        Log.w(TAG, "add contact failed", e);
                        UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, e);
                    }
                }
            }
        }
    }

    private void selectAddContactFragment(Identity identity) {

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, AddContactFragment.newInstance(identity), null)
                .addToBackStack(null)
                .commit();
    }

    private final BroadcastReceiver refreshContactListReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(TAG, "receive refresh contactlist event");
            LocalQabelService service = QabelBoxApplication.getInstance().getService();
            refreshContactList(service.getContacts(service.getActiveIdentity()));
        }
    };

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
