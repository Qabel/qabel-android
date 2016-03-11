package de.qabel.android.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.qabel.core.config.Identity;
import de.qabel.android.R;
import de.qabel.android.helper.QRCodeHelper;

/**
 * Created by danny on 04.02.16.
 */
public class QRcodeFragment extends BaseFragment {

    private static final String ARG_IDENTITY = "Identity";
    private Fragment fragment;
    private Identity identity;
    private TextView editTextContactName;
    private TextView editTextDropURL;
    private TextView editTextPublicKey;

    public static QRcodeFragment newInstance(Identity identity) {

        QRcodeFragment fragment = new QRcodeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITY, identity);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mActivity.toggle.setDrawerIndicatorEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setActionBarBackListener();
        setHasOptionsMenu(true);
        Bundle arguments = getArguments();
        if (arguments != null) {
            identity = (Identity) arguments.getSerializable(ARG_IDENTITY);
        }
        fragment = this;
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

        editTextContactName = (TextView) view.findViewById(R.id.editTextContactName);
        editTextDropURL = (TextView) view.findViewById(R.id.editTextContactDropURL);
        editTextPublicKey = (TextView) view.findViewById(R.id.editTextContactPublicKey);
        editTextContactName.setText(identity.getAlias());
        editTextDropURL.setText(identity.getDropUrls().toArray()[0].toString());
        editTextPublicKey.setText(identity.getKeyIdentifier());
        ImageView imageView = (ImageView) view.findViewById(R.id.qrcode);
        QRCodeHelper.generateQRCode(getActivity(), identity, imageView);
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