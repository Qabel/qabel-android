package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
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

import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.ContactAdapterItem;
import de.qabel.qabelbox.adapter.ContactsAdapter;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Fragment that shows a contact list.
 */
public class ContactFragment extends BaseFragment {

	private static final int REQUEST_IMPORT_CONTACT = 1000;
	private static final int REQUEST_EXPORT_CONTACT = 1001;
	private static final String TAG = "ContactFragment";

	private RecyclerView contactListRecyclerView;
	private ContactsAdapter contactListAdapter;

	private BaseFragment self;
	private TextView contactCount;
	private View emptyView;
	private ChatServer chatServer;
	private String dataToExport;
	private int exportedContactCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		self = this;
		chatServer = mActivity.chatServer;
		setHasOptionsMenu(true);
		mActivity.registerReceiver(refreshContactListReceiver, new IntentFilter(Helper.INTENT_REFRESH_CONTACTLIST));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_contacts, container, false);
		contactCount = (TextView) view.findViewById(R.id.contactCount);
		contactListRecyclerView = (RecyclerView) view.findViewById(R.id.contact_list);
		contactListRecyclerView.setHasFixedSize(true);
		emptyView = view.findViewById(R.id.empty_view);
		RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
		contactListRecyclerView.setLayoutManager(recyclerViewLayoutManager);
		refreshContactList();

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

			new AsyncTask<Void, Void, Collection<DropMessage>>() {
				@Override
				protected Collection<DropMessage> doInBackground(Void... params) {

					return chatServer.refreshList();
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		if (id == R.id.action_contact_export_all) {
			exportAllContacts();

		}
		return super.onOptionsItemSelected(item);
	}

	private void exportAllContacts() {
		try {
			LocalQabelService service = QabelBoxApplication.getInstance().getService();
			Contacts contacts = service.getContacts(service.getActiveIdentity());
			exportedContactCount = contacts.getContacts().size();
			if (exportedContactCount > 0) {
				String contactJson = ContactExportImport.exportContacts(contacts);
				startExportFileChooser("", QabelSchema.FILE_PREFIX_CONTACTS, contactJson);
			}

		} catch (JSONException e) {
			Log.e(TAG, "error on export contacts", e);
			UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_warning, R.string.cant_export_contacts);
		}
	}


	private void startExportFileChooser(String filename, String type, String data) {

		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

		intent.addCategory(Intent.CATEGORY_OPENABLE);

		intent.setType("application/json");
		intent.putExtra(Intent.EXTRA_TITLE, type + "" + filename + "." + QabelSchema.FILE_SUFFIX_CONTACT);
		dataToExport = data;
		startActivityForResult(intent, ContactFragment.REQUEST_EXPORT_CONTACT);
	}

	private void exportContact(Contact contact) {
		exportedContactCount = 1;
		String contactJson = ContactExportImport.exportContact(contact);
		startExportFileChooser(contact.getAlias(), QabelSchema.FILE_PREFIX_CONTACT, contactJson);
	}

	private void setClickListener() {

		contactListAdapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {

			@Override
			public void onItemClick(View view, final int position) {

				final Contact contact = contactListAdapter.getContact(position);
				getFragmentManager().beginTransaction().add(R.id.fragment_container, ContactChatFragment.newInstance(contact), MainActivity.TAG_CONTACT_CHAT_FRAGMENT).addToBackStack(MainActivity.TAG_CONTACT_CHAT_FRAGMENT).commit();
			}
		}, new ContactsAdapter.OnItemClickListener() {

			@Override
			public void onItemClick(View view, final int position) {
				longContactClickAction(position);

			}
		});
	}

	private void longContactClickAction(final int position) {
		final Contact contact = contactListAdapter.getContact(position);
		final LocalQabelService service = QabelBoxApplication.getInstance().getService();
		new BottomSheet.Builder(mActivity).title(contact.getAlias()).sheet(R.menu.bottom_sheet_contactlist)
				.listener(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						switch (which) {
							case R.id.contact_list_item_delete:

								UIHelper.showDialogMessage(getActivity(), getString(R.string.dialog_headline_warning),
										getString(R.string.dialog_message_delete_contact_question).replace("%1", contact.getAlias()),
										R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												service.deleteContact(contact);
												sendRefreshContactList();
												UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_info, getString(R.string.contact_deleted).replace("%1", contact.getAlias()));
											}
										}, null);
								break;
							case R.id.contact_list_item_export:
								exportContact(contact);
								break;
						}
					}
				}).show();
	}

	/**
	 * add contact and show messages
	 *
	 * @param contact
	 */
	//TODO: Remove static
	public static void addContactSilent(Contact contact) throws QblStorageEntityExistsException {
		LocalQabelService service = QabelBoxApplication.getInstance().getService();
		service.addContact(contact);
		sendRefreshContactList();
	}


	private static void sendRefreshContactList() {
		Log.d(TAG, "send refresh intent");
		Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
		QabelBoxApplication.getInstance().getApplicationContext().sendBroadcast(intent);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "unregisterReceiver");
		mActivity.unregisterReceiver(refreshContactListReceiver);
		super.onDestroy();
	}

	private void refreshContactList() {
		if (contactListRecyclerView != null) {
			Contacts contacts = QabelBoxApplication.getInstance().getService().getContacts();
			final int count = contacts.getContacts().size();
			ArrayList<ContactAdapterItem> items = new ArrayList<>();
			for (Contact c : contacts.getContacts()) {
				items.add(new ContactAdapterItem(c, chatServer.hasNewMessages(c)));
			}
			contactListAdapter = new ContactsAdapter(items);
			setClickListener();

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

					if (count == 0) {
						contactCount.setVisibility(View.INVISIBLE);
					} else {
						contactCount.setText(getString(R.string.contact_count).replace("%1", "" + count));
						contactCount.setVisibility(View.VISIBLE);
					}
					contactListAdapter.setEmptyView(emptyView);
					contactListRecyclerView.setAdapter(contactListAdapter);

					contactListAdapter.notifyDataSetChanged();

				}
			});
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
						UIHelper.showDialogMessage(activity, R.string.dialog_headline_info, R.string.contact_export_failed, e);
					}
				}
			}

			if (requestCode == REQUEST_IMPORT_CONTACT) {
				if (resultData != null) {
					Uri uri = resultData.getData();

					try {
						int added = 0;
						ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "r");
						FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
						String json = FileHelper.readFileAsText(fis);
						fis.close();
						Contacts contacts = ContactExportImport.parse(QabelBoxApplication.getInstance().getService().getActiveIdentity(), json);
						for (Contact contact : contacts.getContacts()) {
							try {
								addContactSilent(contact);
								added++;
							} catch (QblStorageEntityExistsException existsException) {
								Log.w(TAG, "found doublet's. Will ignore it", existsException);
							}
						}
						if (added > 0) {
							UIHelper.showDialogMessage(
									mActivity,
									mActivity.getString(R.string.dialog_headline_info),
									mActivity.getResources().getQuantityString(R.plurals.contact_import_successfull, added, added));
						} else {
							UIHelper.showDialogMessage(
									mActivity,
									mActivity.getString(R.string.dialog_headline_info),
									mActivity.getString(R.string.contact_import_zero_additions)
							);
						}
					} catch (IOException | JSONException ioException) {
						UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, ioException);
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
						ContactFragment.addContactSilent(contact);
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
		}
	};

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
			Log.d(TAG, "refreshed ");
			sendRefreshContactList();
		}
	};
}
