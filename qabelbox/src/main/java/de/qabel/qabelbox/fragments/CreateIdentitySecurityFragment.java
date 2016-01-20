package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;

/**
 * Created by danny on 19.01.16.
 */
public class CreateIdentitySecurityFragment extends BaseIdentityFragment {

    private SeekBar sbSecurity;
    private AddIdentityListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_identity_security, container, false);
        sbSecurity = (SeekBar) view.findViewById(R.id.sb_security);
        return view;
    }

    @Override
    public String check() {

        mActivty.setSecurity(sbSecurity.getProgress());
        Identity identity = createIdentity();
        addIdentity(identity);
        mActivty.setCreatedIdentity(identity);
        return null;
    }

    /**
     * create identity
     */
    private Identity createIdentity() {

        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        Toast.makeText(getActivity(), "en " + (mActivty.getSecurity() + 1) * 64, Toast.LENGTH_LONG).show();
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator((mActivty.getSecurity() + 1) * 64);
        DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);

        Identity identity = new Identity(mActivty.getIdentityName(),
                dropURLs, new QblECKeyPair());

        return identity;
    }

    public void addIdentity(Identity identity) {

//        mService.addIdentity(identity);
     /*   changeActiveIdentity(identity);
        provider.notifyRootsUpdated();

        Snackbar.make(appBarMain, "Added identity: " + identity.getAlias(), Snackbar.LENGTH_LONG)
                .show();
        selectFilesFragment();*/
    }

    private void changeActiveIdentity(Identity identity) {

        /*    mService.setActiveIdentity(identity);

        initBoxVolume(identity);
        initFilesFragment();*/
    }

    @Override
    public void resetData() {

        if (sbSecurity != null)
            sbSecurity.setProgress(-1);
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();

        mActivty.setSecurity(Integer.MIN_VALUE);
    }

    public interface AddIdentityListener {

        void addIdentity(Identity identity);

        void cancelAddIdentity();
    }
}
