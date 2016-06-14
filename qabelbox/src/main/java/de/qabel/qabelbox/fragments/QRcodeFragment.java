package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.QRCodeHelper;

/**
 * Created by danny on 04.02.16.
 */
public class QRcodeFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private static final String ARG_CONTACT = "Contact";
    private Identity identity;
    private Contact contact;
    @BindView(R.id.editTextContactName)
    TextView editTextContactName;
    @BindView(R.id.editTextContactDropURL)
    TextView editTextDropURL;
    @BindView(R.id.editTextContactPublicKey)
    TextView editTextPublicKey;
    @BindView(R.id.qrcode)
    ImageView imageView;

    public static QRcodeFragment newInstance(Identity identity) {

        QRcodeFragment fragment = new QRcodeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, identity);
        fragment.setArguments(args);
        return fragment;
    }

    public static QRcodeFragment newInstance(Contact contact) {

        QRcodeFragment fragment = new QRcodeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT, contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(ARG_CONTACT)) {
                contact = (Contact) arguments.getSerializable(ARG_CONTACT);
            } else {
                identity = (Identity) arguments.getSerializable(ARG_IDENTITY);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_barcode_shower, container, false);
        ButterKnife.bind(this, view);

        if (contact != null) {
            editTextContactName.setText(contact.getAlias());
            editTextDropURL.setText(contact.getDropUrls().toArray()[0].toString());
            editTextPublicKey.setText(contact.getKeyIdentifier());
            new QRCodeHelper().generateQRCode(getActivity(), contact, imageView);
        } else {
            editTextContactName.setText(identity.getAlias());
            editTextDropURL.setText(identity.getDropUrls().toArray()[0].toString());
            editTextPublicKey.setText(identity.getKeyIdentifier());
            new QRCodeHelper().generateQRCode(getActivity(), identity, imageView);
        }
        return view;
    }

    @Override
    public boolean isFabNeeded() {

        return false;
    }

    @Override
    public String getTitle() {

        return getString(R.string.headline_qrcode);
    }

    @Override
    public boolean supportBackButton() {

        return true;
    }
}
