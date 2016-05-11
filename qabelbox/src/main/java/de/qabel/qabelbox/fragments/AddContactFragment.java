package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class AddContactFragment extends ContactBaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = this.getClass().getSimpleName();
    private EditText editTextContactName;
    private EditText editTextDropURL;
    private EditText editTextPublicKey;

    private View mView;

    public static AddContactFragment newInstance(Identity identity) {

        AddContactFragment fragment = new AddContactFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, identity);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_add_contact, container, false);

        editTextContactName = (EditText) view.findViewById(R.id.editTextContactName);
        editTextDropURL = (EditText) view.findViewById(R.id.editTextContactDropURL);
        editTextPublicKey = (EditText) view.findViewById(R.id.editTextContactPublicKey);

        mView = view;
        return view;
    }

    private void add() {

        try {
            DropURL dropURL = new DropURL(editTextDropURL.getText().toString());

            Collection<DropURL> dropURLs = new ArrayList<>();
            dropURLs.add(dropURL);

            QblECPublicKey publicKey = new QblECPublicKey(
                    Hex.decode(editTextPublicKey.getText().toString()));
            Contact contact = new Contact(editTextContactName.getText().toString(), dropURLs, publicKey);
            addContactSilent(getActivity().getApplicationContext(), contact);
        } catch (Exception e) {
            Log.w(TAG, "add contact failed", e);
            UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_manual_failed, e);
        }
        UIHelper.hideKeyboard(getActivity(), mView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_add, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_add) {
            add();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isFabNeeded() {

        return false;
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_add_contact);
    }

    @Override
    public boolean supportBackButton() {

        return true;
    }
}
