package de.qabel.qabelbox.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.cocosw.bottomsheet.BottomSheet;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.IdentitiesAdapter;

/**
 * Fragment that shows an identity list.
 */
public class IdentitiesFragment extends BaseFragment {

    private static final String ARG_IDENTITIES = "ARG_IDENTITIES";

    private RecyclerView identityListRecyclerView;
    private IdentitiesAdapter identityListAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;

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

        View view = inflater.inflate(R.layout.fragment_identities, container, false);

        identityListRecyclerView = (RecyclerView) view.findViewById(R.id.identity_list);
        identityListRecyclerView.setHasFixedSize(true);

        recyclerViewLayoutManager = new LinearLayoutManager(view.getContext());
        identityListRecyclerView.setLayoutManager(recyclerViewLayoutManager);

        identityListAdapter = new IdentitiesAdapter(identities);
        identityListAdapter.sort();
        identityListRecyclerView.setAdapter(identityListAdapter);

        identityListAdapter.setOnItemClickListener(new IdentitiesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final Identity identity = identityListAdapter.get(position);
                new BottomSheet.Builder(activity).title(identity.getAlias()).sheet(R.menu.identities_bottom_sheet)
                        .listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.identities_rename:
                                        AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);

                                        renameDialog.setTitle(
                                                String.format(getString(R.string.rename_identity), identity.getAlias()));
                                        renameDialog.setMessage(R.string.new_identity_name);

                                        final EditText editTextNewAlias = new EditText(activity);
                                        renameDialog.setView(editTextNewAlias);

                                        renameDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                String newAlias = editTextNewAlias.getText().toString();
                                                if (newAlias.equals("")) {
                                                    Toast.makeText(activity, R.string.alias_cannot_be_empty, Toast.LENGTH_LONG)
                                                            .show();
                                                } else {
                                                    identity.setAlias(newAlias);
                                                    mListener.modifyIdentity(identity);
                                                    identityListAdapter.sort();
                                                    identityListAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });

                                        renameDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                            }
                                        });
                                        renameDialog.show();
                                        break;
                                    case R.id.identities_delete:
                                        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(activity);

                                        confirmDelete.setTitle(R.string.confirm_delete_identity_header);
                                        confirmDelete.setMessage(
                                                String.format(getString(R.string.confirm_delete_identity_message)
                                                        , identity.getAlias()));

                                        confirmDelete.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                mListener.deleteIdentity(identity);
                                                identityListAdapter.remove(identity);
                                                identityListAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        confirmDelete.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                            }
                                        });
                                        confirmDelete.show();
                                        break;
                                    case R.id.identities_export:
                                        Toast.makeText(activity, R.string.not_implemented,
                                                Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        }).show();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (IdentityListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IdentityListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface IdentityListListener {
        void deleteIdentity(Identity identity);
        void modifyIdentity(Identity identity);
    }

    @Override
    public boolean isFabNeeded() {
        return true;
    }

    public boolean supportBackButton() {
        return false;
    }
}
