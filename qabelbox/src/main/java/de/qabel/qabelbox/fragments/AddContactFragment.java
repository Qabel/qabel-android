package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Activities that contain this fragment must implement the
 * {@link ContactFragment.ContactListListener} interface
 * to handle interaction events.
 */
public class AddContactFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private final String TAG = this.getClass().getSimpleName();
    private Fragment fragment;
    private Identity identity;
    private EditText editTextContactName;
    private EditText editTextDropURL;
    private EditText editTextPublicKey;
    private Button buttonAdd;

    private ContactFragment.ContactListListener mListener;

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

        Bundle arguments = getArguments();
        if (arguments != null) {
            identity = (Identity) arguments.getSerializable(ARG_IDENTITY);
        }
        actionBar.setDisplayHomeAsUpEnabled(true);

        fragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_add_contact, container, false);

        editTextContactName = (EditText) view.findViewById(R.id.editTextContactName);
        editTextDropURL = (EditText) view.findViewById(R.id.editTextContactDropURL);
        editTextPublicKey = (EditText) view.findViewById(R.id.editTextContactPublicKey);
        buttonAdd = (Button) view.findViewById(R.id.buttonAddContact);


        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    DropURL dropURL = new DropURL(editTextDropURL.getText().toString());

                    Collection<DropURL> dropURLs = new ArrayList<>();
                    dropURLs.add(dropURL);

                    QblECPublicKey publicKey = new QblECPublicKey(
                            Hex.decode(editTextPublicKey.getText().toString()));
                    Contact contact = new Contact(identity, editTextContactName.getText().toString(), dropURLs, publicKey);
                    ContactFragment.addContact(mActivity, contact);
                } catch (Exception e) {
                    Log.w(TAG, "add contact failed", e);
                    UIHelper.showDialogMessage(mActivity, R.string.dialog_headline_warning, R.string.contact_import_failed, e);
                }

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });



        return view;
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        try {
            mListener = (ContactFragment.ContactListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddContactListener");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null && scanResult.getContents() != null) {
            try {
                mListener.contactAdded(ContactExportImport.parseContactForIdentity(identity, scanResult.getContents()));
            } catch (JSONException | URISyntaxException | QblDropInvalidURL e) {
                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.cant_read_contact);
            }
        }
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
