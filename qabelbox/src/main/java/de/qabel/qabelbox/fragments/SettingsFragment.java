package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.View;

import net.hockeyapp.android.FeedbackManager;

import org.apache.commons.io.FileUtils;

import javax.inject.Inject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.BaseActivity;
import de.qabel.qabelbox.communication.callbacks.JSONModelCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.model.BoxQuota;
import de.qabel.qabelbox.storage.server.BlockServer;
import okhttp3.Response;

public class SettingsFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "settings";

    @Inject
    AppPreference preferences;
    @Inject
    BlockServer blockServer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        findPreference(getString(R.string.settings_key_change_password)).setOnPreferenceClickListener(preference -> {
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_content,
                    new ChangeBoxAccountPasswordFragment()).addToBackStack(null).commit();
            return true;
        });

        findPreference(getString(R.string.settings_key_internal_feedback)).setOnPreferenceClickListener(preference -> {
            FeedbackManager.register(getActivity(), getString(R.string.hockeykey), null);
            FeedbackManager.showFeedbackActivity(getActivity());
            return true;
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((BaseActivity)getActivity()).getApplicationComponent().inject(this);

        BoxQuota quota = preferences.getBoxQuota();
        if (quota.getQuota() < 0) {
            blockServer.getQuota(new JSONModelCallback<BoxQuota>() {
                @Override
                protected BoxQuota createModel() {
                    return new BoxQuota();
                }

                @Override
                protected void onSuccess(Response response, BoxQuota model) {
                    preferences.setBoxQuota(model);
                    getActivity().runOnUiThread(() -> refreshQuota());
                }

                @Override
                protected void onError(Exception e, @Nullable Response response) {
                    refreshQuota();
                }
            });
        } else {
            refreshQuota();
        }
    }

    private void refreshQuota() {
        BoxQuota quota = preferences.getBoxQuota();
        String summaryLabel;
        String usedStorage = FileUtils.byteCountToDisplaySize(quota.getSize());
        if(quota.getQuota() < 0){
            summaryLabel = getString(R.string.currently_not_available);
        }else if (quota.getQuota() > 0) {
            summaryLabel = getString(R.string.used_storage, usedStorage,
                    FileUtils.byteCountToDisplaySize(quota.getQuota()));
        } else {
            summaryLabel = getString(R.string.unlimited_storage, usedStorage);
        }
        findPreference("boxquota").setSummary(summaryLabel);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}

