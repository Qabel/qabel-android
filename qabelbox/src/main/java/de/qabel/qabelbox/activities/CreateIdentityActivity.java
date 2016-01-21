package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

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
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateIdentityDropBitsFragment;
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment;
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import de.qabel.qabelbox.services.LocalQabelService;

/**
 * Created by danny on 11.01.2016.
 */
public class CreateIdentityActivity extends BaseWizwardActivity {

    private String TAG = this.getClass().getSimpleName();

    private String mIdentityName;
    private int mIdentityDropProgress = Integer.MIN_VALUE;
    private Identity mNewIdentity;

    @Override
    protected String getHeaderFragmentText() {

        return mIdentityName;
    }

    @Override
    protected int getActionBarTitle() {

        return R.string.headline_add_identity;
    }

    /**
     * fill fragment list with fragments to navigate via wizard
     */
    @Override
    protected BaseIdentityFragment[] getFragmentList() {

        BaseIdentityFragment fragments[] = new BaseIdentityFragment[4];
        fragments[0] = new CreateIdentityMainFragment();
        fragments[1] = CreateIdentityEditTextFragment.newInstance(R.string.create_identity_enter_name, R.string.create_identity_enter_name_hint, new NextChecker() {
            @Override
            public String check(View view) {

                String editText = ((EditText) view).getText().toString().trim();
                boolean error = editText.length() < 1;
                if (error) {
                    return getString(R.string.create_identity_enter_all_data);
                }
                //@todo add function to check if identity existis

                setIdentityName(editText);
                return null;
            }
        });
        fragments[2] = CreateIdentityDropBitsFragment.newInstance(new NextChecker() {
            @Override
            public String check(View view) {

                setIdentityDropBitsProgress(((SeekBar) view).getProgress());
                Identity identity = createIdentity();
                addIdentity(identity);
                setCreatedIdentity(identity);
                return null;
            }

            private Identity createIdentity() {

                URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
                DropServer dropServer = new DropServer(uri, "", true);
                DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator((getIdentityDropBitsProgress() + 1) * 64);
                DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
                Collection<DropURL> dropURLs = new ArrayList<>();
                dropURLs.add(dropURL);

                return new Identity(getIdentityName(),
                        dropURLs, new QblECKeyPair());
            }

            private void addIdentity(Identity identity) {

                LocalQabelService mService = QabelBoxApplication.getInstance().getService();
                mService.addIdentity(identity);
            }
        });

        fragments[3] = new CreateIdentityFinalFragment();
        return fragments;
    }

    @Override
    protected void completeWizard() {

        Intent result = new Intent();
        result.putExtra(P_IDENTITY, mNewIdentity);
        setResult(activityResult, result);
        finish();
        if (mFirstRun) {
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.setAction("");
            startActivity(intent);
        }
    }

    public void setIdentityName(String text) {

        mIdentityName = text;
    }

    public String getIdentityName() {

        return mIdentityName;
    }

    public void setIdentityDropBitsProgress(int progress) {

        mIdentityDropProgress = progress;
    }

    public int getIdentityDropBitsProgress() {

        return mIdentityDropProgress;
    }

    public void setCreatedIdentity(Identity identity) {

        mNewIdentity = identity;
    }
}
