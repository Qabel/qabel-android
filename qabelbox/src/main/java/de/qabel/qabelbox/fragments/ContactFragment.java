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

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.ContactAdapterItem;
import de.qabel.qabelbox.adapter.ContactsAdapter;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.dagger.components.MainActivityComponent;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.helper.ExternalApps;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.ui.views.ChatFragment;

/**
 * Fragment that shows a contact list.
 */
public class ContactFragment extends BaseFragment {

    public static final int REQUEST_IMPORT_CONTACT = 1000;
    public static final int REQUEST_EXPORT_CONTACT = 1001;
    private static final String TAG = "ContactFragment";

    @BindView(R.id.contact_list) RecyclerView contactListRecyclerView;
    private ContactsAdapter contactListAdapter;

    @BindView(R.id.contactCount) TextView contactCount;

    @BindView(R.id.empty_view) View emptyView;
    private String dataToExport;
    private int exportedContactCount;
    private boolean useDocumentProvider = true;//used for tests

    @Inject ContactRepository contactRepository;

    @Inject Context context;

    @Inject ChatServer chatServer;

    @Inject Identity activeIdentity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getComponent(MainActivityComponent.class).inject(this);
        setHasOptionsMenu(true);
        startRefresh();
        refreshContactList();
    }

    private Identity getActiveIdentity() {
        return activeIdentity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        ButterKnife.bind(this, view);
        contactListRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_contacts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_contact_refresh) {
            startRefresh();
        }
        if (id == R.id.action_contact_export_all) {
            exportAllContacts();

        }
        return super.onOptionsItemSelected(item);
    }

    public void startRefresh() {
        AccountHelper.startOnDemandSyncAdapter();
    }

    public void enableDocumentProvider(boolean value) {
        useDocumentProvider = value;
    }

    public void exportAllContacts() {
        try {
            Contacts contacts = contactRepository.find(getActiveIdentity());
            exportedContactCount = contacts.getContacts().size();
            if (exportedContactCount > 0) {
                String contactJson = ContactExportImport.exportContacts(contacts);
                startExportFileChooser("", QabelSchema.FILE_PREFIX_CONTACTS, contactJson);
            }

        } catch (PersistenceException | JSONException e) {
            Log.e(TAG, "error on export contacts", e);
            UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_warning, R.string.cant_export_contacts);
        }
    }


    private void startExportFileChooser(String filename, String type, String data) {
        dataToExport = data;
        if (useDocumentProvider) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, type + "" + filename + "." + QabelSchema.FILE_SUFFIX_CONTACT);
            startActivityForResult(intent, ContactFragment.REQUEST_EXPORT_CONTACT);
        }
    }

    public void exportContact(Contact contact) {
        exportedContactCount = 1;
        String contactJson = ContactExportImport.exportContact(contact);
        startExportFileChooser(contact.getAlias(), QabelSchema.FILE_PREFIX_CONTACT, contactJson);

    }

    private String createContactFilename(String contactAlias) {
        return QabelSchema.FILE_PREFIX_CONTACT + FileHelper.processFilename(contactAlias) +
                "." + QabelSchema.FILE_SUFFIX_CONTACT;
    }

    public void exportContactToExternal(Contact contact) {
        try {
            String contactJson = ContactExportImport.exportContact(contact);
            File tmpFile = new File(mActivity.getExternalCacheDir(), createContactFilename(contact.getAlias()));
            FileUtils.writeStringToFile(tmpFile, contactJson);

            ExternalApps.share(mActivity, Uri.fromFile(tmpFile), "application/json");
        } catch (IOException | RuntimeException e) {
            UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_export_failed, e);
        }
    }

    private void setClickListener() {

        contactListAdapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final Contact contact = contactListAdapter.getContact(position);
                ContactFragment.this.getFragmentManager().beginTransaction().add(R.id.fragment_container,
                        ChatFragment.Companion.withContact(contact),
                        MainNavigator.TAG_CONTACT_CHAT_FRAGMENT)
                        .addToBackStack(MainNavigator.TAG_CONTACT_CHAT_FRAGMENT).commit();
            }
        }, new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ContactFragment.this.longContactClickAction(position);
            }
        });
    }

    private void longContactClickAction(final int position) {
        final Contact contact = contactListAdapter.getContact(position);
        new BottomSheet.Builder(mActivity).title(contact.getAlias()).sheet(R.menu.bottom_sheet_contactlist)
                .listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.id.contact_list_item_delete:
                                ContactFragment.this.deleteContact(contact);
                                break;
                            case R.id.contact_list_item_export:
                                ContactFragment.this.exportContact(contact);
                                break;
                            case R.id.contact_list_item_qrcode:
                                ContactFragment.this.exportContactAsQRCode(contact);
                                break;
                            case R.id.contact_list_item_send:
                                ContactFragment.this.exportContactToExternal(contact);
                                break;
                        }
                    }
                }).show();
    }

    private void deleteContact(final Contact contact) {
        UIHelper.showDialogMessage(getActivity(), getString(R.string.dialog_headline_warning),
                getString(R.string.dialog_message_delete_contact_question).replace("%1", contact.getAlias()),
                R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which1) {
                        try {
                            Log.i(TAG, "Deleting contact " + contact.getId());
                            contactRepository.delete(contact, ContactFragment.this.getActiveIdentity());
                        } catch (EntityNotFoundExcepion | PersistenceException e) {
                            throw new RuntimeException(e);
                        }
                        ContactFragment.this.sendRefreshContactList();
                        UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_info, ContactFragment.this.getString(R.string.contact_deleted).replace("%1", contact.getAlias()));
                    }
                }, null);
    }

    @Override
    public void onResume() {
		super.onResume();
        startRefresh();
    }

    private void exportContactAsQRCode(Contact contact) {
        mActivity.getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, QRcodeFragment.newInstance(contact), null)
                .addToBackStack(null)
                .commit();
    }

    /**
     * add contact and show messages
     *
     * @param contact
     */
    public void addContactSilent(Contact contact) throws QblStorageEntityExistsException, PersistenceException {
        contactRepository.save(contact, getActiveIdentity());
        sendRefreshContactList();
    }


    private void sendRefreshContactList() {
        Log.d(TAG, "send refresh intent");
        busy();
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent);
    }

    private void refreshContactList() {
        Contacts contacts;
        try {
            contacts = contactRepository.find(getActiveIdentity());
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        final int count = contacts.getContacts().size();
        ArrayList<ContactAdapterItem> items = new ArrayList<>();
        for (Contact c : contacts.getContacts()) {
            items.add(new ContactAdapterItem(c, chatServer.hasNewMessages(getActiveIdentity(), c)));
        }
        contactListAdapter = new ContactsAdapter(items);
        setClickListener();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (count == 0) {
                    contactCount.setVisibility(View.INVISIBLE);
                } else {
                    contactCount.setText(ContactFragment.this.getString(R.string.contact_count).replace("%1", "" + count));
                    contactCount.setVisibility(View.VISIBLE);
                }
                contactListAdapter.setEmptyView(emptyView);
                contactListRecyclerView.setAdapter(contactListAdapter);

                contactListAdapter.notifyDataSetChanged();

            }
        });
        idle();
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
                                ContactFragment.this.addContactByFile();
                                break;
                            case R.id.add_contact_via_qr:
                                IntentIntegrator integrator = new IntentIntegrator(ContactFragment.this);
                                integrator.initiateScan();
                                break;
                            case R.id.add_contact_direct_input:
                                ContactFragment.this.selectAddContactFragment(getActiveIdentity());
                                break;
                        }
                    }
                }).show();

        return true;
    }

    private void addContactByFile() {

        if (useDocumentProvider) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_IMPORT_CONTACT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Log.d(TAG, "ContactFragment onActivityResult");
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_EXPORT_CONTACT) {
                if (resultData != null) {
                    Uri uri = resultData.getData();
                    Activity activity = getActivity();
                    try (ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "w");
                         FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor())) {
                        fileOutputStream.write(dataToExport.getBytes());
                        String message = getResources().getQuantityString(R.plurals.contact_export_successfully, exportedContactCount, exportedContactCount);
                        UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, message);

                    } catch (IOException | NullPointerException e) {
                        UIHelper.showDialogMessage(activity, R.string.dialog_headline_warning, R.string.contact_export_failed, e);
                    }
                }
            }

            if (requestCode == REQUEST_IMPORT_CONTACT) {
                if (resultData != null) {
                    Uri uri = resultData.getData();

                    try {
                        ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "r");
                        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                        String json = FileHelper.readFileAsText(fis);
                        fis.close();
                        ContactExportImport.ContactsParseResult contactsParseResult =
                                ContactExportImport.parse(getActiveIdentity(), json);
                        int added = 0;
                        int failed = contactsParseResult.getSkippedContacts();
                        for (Contact contact : contactsParseResult.getContacts().getContacts()) {
                            try {
                                addContactSilent(contact);
                                added++;
                            } catch (PersistenceException | QblStorageEntityExistsException e) {
                                failed++;
                                Log.w(TAG, "found doublette. Will ignore it", e);
                            }
                        }
                        if (added > 0) {
                            if (added == 1 && failed == 0) {
                                UIHelper.showDialogMessage(
                                        mActivity,
                                        mActivity.getString(R.string.dialog_headline_info),
                                        mActivity.getResources().getString(R.string.contact_import_successfull)
                                );
                            } else {
                                UIHelper.showDialogMessage(
                                        mActivity,
                                        mActivity.getString(R.string.dialog_headline_info),
                                        mActivity.getResources().getString(R.string.contact_import_successfull_many, added, (added + failed))
                                );
                            }

                        } else {
                            UIHelper.showDialogMessage(
                                    mActivity,
                                    mActivity.getString(R.string.dialog_headline_info),
                                    mActivity.getString(R.string.contact_import_zero_additions)
                            );
                        }
                        refreshContactList();
                    } catch (IOException | JSONException ioException) {
                        UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, ioException);
                    }
                }
            }

            Log.d(TAG, "Checking for QR code scan");
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
                        addContactSilent(contact);
                        refreshContactList();
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
            refreshContactList();
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    };

    private final BroadcastReceiver showNotificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Aborting chat notification");
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        chatServer.addListener(chatServerCallback);
        IntentFilter filter = new IntentFilter(Helper.INTENT_SHOW_NOTIFICATION);
        filter.setPriority(10);
        mActivity.registerReceiver(showNotificationReceiver,
                filter);
        IntentFilter refreshFilter = new IntentFilter(Helper.INTENT_REFRESH_CONTACTLIST);
        mActivity.registerReceiver(refreshContactListReceiver, refreshFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        chatServer.removeListener(chatServerCallback);
        mActivity.unregisterReceiver(refreshContactListReceiver);
        mActivity.unregisterReceiver(showNotificationReceiver);
    }

    private final ChatServer.ChatServerCallback chatServerCallback = new ChatServer.ChatServerCallback() {
        @Override
        public void onRefreshed() {
            Log.d(TAG, "refreshed ");
            ContactFragment.this.sendRefreshContactList();
        }
    };
}
