package de.qabel.qabelbox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.PrefixServer;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.fragments.BaseIdentityFragment;
import de.qabel.qabelbox.fragments.CreateIdentityDropBitsFragment;
import de.qabel.qabelbox.fragments.CreateIdentityEditTextFragment;
import de.qabel.qabelbox.fragments.CreateIdentityFinalFragment;
import de.qabel.qabelbox.fragments.CreateIdentityMainFragment;
import de.qabel.qabelbox.services.LocalQabelService;
import okhttp3.Response;

/**
 * Created by danny on 11.01.2016.
 */
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
    private int mIdentityDropProgress = Integer.MIN_VALUE;
    private Identity mNewIdentity;
    private String prefix = null;
    int tryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (QabelBoxApplication.getInstance().getService().getIdentities().getIdentities().size() > 0) {
            canExit = true;
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

                Identities identities = QabelBoxApplication.getInstance().getService().getIdentities();
                if (identities != null) {
                    for (Identity identity : identities.getIdentities()) {
                        if (identity.getAlias().equals(editText)) {
                            return getString(R.string.create_identity_already_exists);
                        }
                    }
                }
                setIdentityName(editText);
                return null;
            }
        });
        fragments[2] = CreateIdentityDropBitsFragment.newInstance(new NextChecker() {
            @Override
            public String check(View view) {

                if (prefix == null) {
                    tryCount = 0;
                    loadPrefixInBackground();
                    return getString(R.string.create_idenity_cant_get_prefix);
                }
                setIdentityDropBitsProgress(((SeekBar) view).getProgress());
                Identity identity = createIdentity();
                addIdentity(identity);
                setCreatedIdentity(identity);

                return null;
            }

            private Identity createIdentity() {

                URI uri = URI.create(getString(R.string.dropServer));
                DropServer dropServer = new DropServer(uri, "", true);
                DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator((getIdentityDropBitsProgress() + 1) * 64);
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

                LocalQabelService mService = QabelBoxApplication.getInstance().getService();
                mService.addIdentity(identity);
                mService.setActiveIdentity(identity);
            }
        });

        fragments[3] = new CreateIdentityFinalFragment();
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

    public void setIdentityDropBitsProgress(int progress) {

        mIdentityDropProgress = progress;
    }

    public int getIdentityDropBitsProgress() {

        return mIdentityDropProgress;
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
