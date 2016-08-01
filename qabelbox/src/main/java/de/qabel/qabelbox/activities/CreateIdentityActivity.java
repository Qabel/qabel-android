package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.exception.PersistenceException;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.PrefixServer;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment;
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import okhttp3.Response;

public class CreateIdentityActivity extends BaseWizardActivity {

    public static final int REQUEST_CODE_IMPORT_IDENTITY = 1;
    /**
     * Fake the prefix request
     *
     * This is set by the QblJUnitTestRunner to prevent network requests to the block server
     * in test runs.
     */
    public static boolean FAKE_COMMUNICATION = false;

    private String TAG = this.getClass().getSimpleName();

    private String mIdentityName;
    private Identity mNewIdentity;
    private String prefix = null;
    int tryCount = 0;

    @Inject
    IdentityRepository identityRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        QabelBoxApplication.getApplicationComponent(getApplicationContext()).inject(this);

        super.onCreate(savedInstanceState);
        try {
            if (identityRepository.findAll().getIdentities().size() > 0) {
                canExit = true;
            }
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected String getHeaderFragmentText() {

        return mIdentityName;
    }

    @Override
    protected int getActionBarTitle() {

        return R.string.headline_add_identity;
    }

    @Override
    protected String getWizardEntityLabel() {
        return getString(R.string.identity);
    }

    @Override
    public void handleNextClick() {

        super.handleNextClick();
        if (step > 0 && tryCount != 3 && prefix == null) {
            loadPrefixInBackground();
        }
    }

    /**
     * fill fragment list with fragments to navigate via wizard
     */
    @Override
    protected BaseIdentityFragment[] getFragmentList() {

        BaseIdentityFragment fragments[] = new BaseIdentityFragment[3];
        fragments[0] = new CreateIdentityMainFragment();
        fragments[1] = CreateIdentityEditTextFragment.newInstance(R.string.create_identity_enter_name, R.string.create_identity_enter_name_hint, new NextChecker() {
            @Override
            public String check(View view) {

                String editText = ((EditText) view).getText().toString().trim();
                boolean error = editText.length() < 1;
                if (error) {
                    return getString(R.string.create_identity_enter_all_data);
                }

                Identities identities = null;
                try {
                    identities = identityRepository.findAll();
                } catch (PersistenceException e) {
                    throw new RuntimeException(e);
                }
                if (identities != null) {
                    for (Identity identity : identities.getIdentities()) {
                        if (identity.getAlias().equals(editText)) {
                            return getString(R.string.create_identity_already_exists);
                        }
                    }
                }
                setIdentityName(editText);

                if (prefix == null) {
                    tryCount = 0;
                    loadPrefixInBackground();
                    return getString(R.string.create_idenity_cant_get_prefix);
                }
                Identity identity = createIdentity();
                addIdentity(identity);
                setCreatedIdentity(identity);

                return null;
            }

            private Identity createIdentity() {

                URI uri = URI.create(getString(R.string.dropServer));
                DropServer dropServer = new DropServer(uri, "", true);
                DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(64);
                DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
                Collection<DropURL> dropURLs = new ArrayList<>();
                dropURLs.add(dropURL);
                Identity identity = new Identity(getIdentityName(),
                        dropURLs, new QblECKeyPair());
                Log.d(TAG, "add prefix to identity: " + prefix);
                identity.getPrefixes().add(prefix);
                return identity;
            }

            private void addIdentity(Identity identity) {
                try {
                    identityRepository.save(identity);
                } catch (PersistenceException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        fragments[2] = new CreateIdentityFinalFragment();
        return fragments;
    }

    @Override
    public void completeWizard() {

        Intent result = new Intent();
        result.putExtra(P_IDENTITY, mNewIdentity);
        setResult(activityResult, result);
        if (FAKE_COMMUNICATION) {
            return;
        }
        finish();
        if (mFirstRun) {
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, mNewIdentity.getKeyIdentifier());
            startActivity(intent);
        }
    }

    public void setIdentityName(String text) {

        mIdentityName = text;
    }

    public String getIdentityName() {

        return mIdentityName;
    }

    public void setCreatedIdentity(Identity identity) {

        mNewIdentity = identity;
    }

    private void loadPrefixInBackground() {
        if (FAKE_COMMUNICATION) {
            prefix = TestConstants.PREFIX;
            return;
        }

        if (tryCount < 3) {
            PrefixServer prefixServer = new PrefixServer(getApplicationContext());
            prefixServer.getPrefix(this, new JsonRequestCallback(new int[]{201}) {

                @Override
                protected void onError(Exception e, @Nullable Response response) {
                    Log.d(TAG, "Server communication failed: ", e);
                    tryCount++;
                    loadPrefixInBackground();
                }

                @Override
                protected void onJSONSuccess(Response response, JSONObject result) {
                    try {
                        Log.d(TAG, "Server response code: " + response.code());
                        prefix = result.getString("prefix");
                    } catch (JSONException e) {
                        Log.e(TAG, "Cannot parse prefix from server", e);
                        tryCount++;
                        loadPrefixInBackground();
                    }
                }
            });
        }
    }
}
