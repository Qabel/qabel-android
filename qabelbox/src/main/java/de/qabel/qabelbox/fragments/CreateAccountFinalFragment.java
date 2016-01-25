package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountFinalFragment extends BaseIdentityFragment {

    private TextView tvSuccess, tvMessage;
    boolean needCreateIdentity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_create_identity_final, container, false);
        tvSuccess = (TextView) v.findViewById(R.id.tv_success);
        tvMessage = (TextView) v.findViewById(R.id.tv_message);

        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        Identities identities = service.getIdentities();
        if (identities == null || identities.getIdentities().size() == 0) {
            //no identities availible.
            needCreateIdentity = true;
        } else {
            //identities availible. check if one active.
            Identity activeIdentity = service.getActiveIdentity();
            if (activeIdentity == null) {
                //no active identities. try to set the first as active
                Identity identityToSet = identities.getIdentities().iterator().next();
                service.setActiveIdentity(identityToSet);
                Identity setted = service.getActiveIdentity();
                if (setted != null && setted.getAlias().equals(activeIdentity.getAlias())) {
                    //active identity setted
                    needCreateIdentity = false;
                } else {
                    needCreateIdentity = true;
                }
            }
            if (needCreateIdentity) {
                tvMessage.setText(R.string.create_identity_final_create_identity);
            }
        }
        return v;
    }

    @Override
    public String check() {

        return null;
    }
}
