package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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

/**
 * Activities that contain this fragment must implement the
 * {@link AddContactFragment.AddContactListener} interface
 * to handle interaction events.
 */
public class AddContactFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private Fragment fragment;
    private Identity identity;
    private EditText editTextContactName;
    private EditText editTextDropURL;
    private EditText editTextPublicKey;
    private Button buttonAdd;
    private Button buttonScan;

    private AddContactListener mListener;

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
        buttonScan = (Button) view.findViewById(R.id.buttonScanContact);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    DropURL dropURL = new DropURL(editTextDropURL.getText().toString());

                    Collection<DropURL> dropURLs = new ArrayList<>();
                    dropURLs.add(dropURL);

                    QblECPublicKey publicKey = new QblECPublicKey(
                            Hex.decode(editTextPublicKey.getText().toString()));
                    mListener.addContact(
                            new Contact(identity, editTextContactName.getText().toString(), dropURLs, publicKey));
                } catch (URISyntaxException e) {
                    Toast.makeText(getActivity(), R.string.invalid_drop_url, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (QblDropInvalidURL e) {
                    Toast.makeText(getActivity(), R.string.invalid_drop_url, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (DecoderException e) {
                    Toast.makeText(getActivity(), R.string.invalid_public_key, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(fragment);
                integrator.initiateScan();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (AddContactListener) activity;
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
            String[] result = scanResult.getContents().split("\\r?\\n");
            if (result.length == 4 && result[0].equals("QABELCONTACT")) {
                try {
                    DropURL dropURL = new DropURL(result[2]);
                    Collection<DropURL> dropURLs = new ArrayList<>();
                    dropURLs.add(dropURL);

                    QblECPublicKey publicKey = new QblECPublicKey(Hex.decode(result[3]));
                    mListener.addContact(new Contact(identity, result[1], dropURLs, publicKey));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (QblDropInvalidURL e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isFabNeeded() {
        return false;
    }

    public interface AddContactListener {
        void addContact(Contact contact);
    }

    @Override
    public boolean supportBackButton() {
        return false;
    }


}
