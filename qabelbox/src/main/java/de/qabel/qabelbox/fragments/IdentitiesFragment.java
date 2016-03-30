package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.cocosw.bottomsheet.BottomSheet;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.menu;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.adapter.IdentitiesAdapter;
import de.qabel.qabelbox.adapter.IdentitiesAdapter.OnItemClickListener;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.config.IdentityExportImport;
import de.qabel.qabelbox.config.QabelSchema;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.helper.UIHelper.EditTextDialogClickListener;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Fragment that shows an identity list.
 */
public class IdentitiesFragment extends BaseFragment {

    private static final String ARG_IDENTITIES = "ARG_IDENTITIES";

    private RecyclerView identityListRecyclerView;
    private IdentitiesAdapter identityListAdapter;
    private LayoutManager recyclerViewLayoutManager;

    private Identity identityToExport;
    private Identities identities;
    private IdentityListListener mListener;

    private Fragment self;
    private Activity activity;

    public static IdentitiesFragment newInstance(Identities identities) {

        IdentitiesFragment fragment = new IdentitiesFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITIES, identities);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        Bundle arguments = getArguments();
        if (arguments != null) {
            identities = (Identities) arguments.getSerializable(ARG_IDENTITIES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(layout.fragment_identities, container, false);

        identityListRecyclerView = (RecyclerView) view.findViewById(id.identity_list);
        identityListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        identityListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        identityListAdapter = new IdentitiesAdapter(identities);
        identityListAdapter.sort();
        identityListRecyclerView.setAdapter(identityListAdapter);

        identityListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                final Identity identity = identityListAdapter.get(position);
                new BottomSheet.Builder(activity).title(identity.getAlias()).sheet(menu.bottom_sheet_identities)
                        .listener(new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                switch (which) {
                                    case id.identities_rename:

                                        UIHelper.showEditTextDialog(getActivity(), String.format(getString(string.rename_identity), identity.getAlias()), getString(string.new_identity_name), string.ok, string.cancel, new EditTextDialogClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which, EditText editText) {

                                                String newAlias = editText.getText().toString();
                                                if (newAlias.equals("")) {
                                                    Toast.makeText(activity, string.alias_cannot_be_empty, Toast.LENGTH_LONG)
                                                            .show();
                                                } else {
                                                    identity.setAlias(newAlias);
                                                    mListener.modifyIdentity(identity);
                                                    identityListAdapter.sort();
                                                    identityListAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        }, null);
                                        break;
                                    case id.identities_delete:
                                        Builder confirmDelete = new Builder(activity);

                                        confirmDelete.setTitle(string.confirm_delete_identity_header);
                                        confirmDelete.setMessage(
                                                String.format(getString(string.confirm_delete_identity_message)
                                                        , identity.getAlias()));

                                        confirmDelete.setPositiveButton(string.ok, new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                                mListener.deleteIdentity(identity);
                                                identityListAdapter.remove(identity);
                                                identityListAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        confirmDelete.setNegativeButton(string.cancel, new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                            }
                                        });
                                        confirmDelete.show();
                                        break;
                                    case id.identities_export:
                                        exportIdentity(identity);
                                        break;
                                    case id.identities_export_as_contact:
                                        exportIdentityAsContact(identity);
                                        break;
                                    case id.identities_export_as_contact_qrcode:
                                        MainActivity.showQRCode(mActivity, identity);

                                        //QRCodeHelper.exportIdentityAsContactWithQR(getActivity(), identity);
                                }
                            }
                        }).show();
            }
        });

        return view;
    }


    private void exportIdentity(Identity identity) {

        startExportFileChooser(identity, QabelSchema.FILE_PREFIX_IDENTITY, QabelSchema.FILE_SUFFIX_IDENTITY, MainActivity.REQUEST_EXPORT_IDENTITY);
    }

    private void exportIdentityAsContact(Identity identity) {

        startExportFileChooser(identity, QabelSchema.FILE_PREFIX_CONTACT, QabelSchema.FILE_SUFFIX_CONTACT, MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT);
    }

    private void startExportFileChooser(Identity identity, String type, String filesuffix, int requestCode) {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, type + "" + identity.getAlias() + "." + filesuffix);
        //TODO: Is there any way to add data to the intent? Abusing a member for this is so wrong...
        identityToExport = identity;
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MainActivity.REQUEST_EXPORT_IDENTITY || requestCode == MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT) {
                if (resultData != null) {
                    Uri uri = resultData.getData();

                    try (ParcelFileDescriptor pfd = mActivity.getContentResolver().openFileDescriptor(uri, "w");
                         FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor())) {
                        if (requestCode == MainActivity.REQUEST_EXPORT_IDENTITY_AS_CONTACT) {
                            fileOutputStream.write(ContactExportImport.exportIdentityAsContact(identityToExport).getBytes());
                            UIHelper.showDialogMessage(activity, string.dialog_headline_info, string.contact_export_successfully);
                        } else {
                            fileOutputStream.write(IdentityExportImport.exportIdentity(identityToExport).getBytes());
                            UIHelper.showDialogMessage(activity, string.dialog_headline_info, string.identity_export_successfully);
                        }
                    } catch (IOException e) {
                        UIHelper.showDialogMessage(activity, string.dialog_headline_info, string.identity_export_failed, e);
                    }
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (IdentityListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity
                    + " must implement IdentityListListener");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        mListener = null;
    }

    @Override
    public String getTitle() {

        return getString(string.headline_identities);
    }

    public interface IdentityListListener {

        void deleteIdentity(Identity identity);

        void modifyIdentity(Identity identity);
    }

    @Override
    public boolean isFabNeeded() {

        return true;
    }

    @Override
    public boolean supportBackButton() {

        return false;
    }
}
