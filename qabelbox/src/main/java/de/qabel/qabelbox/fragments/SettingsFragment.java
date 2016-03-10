package de.qabel.qabelbox.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import net.hockeyapp.android.FeedbackManager;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.ProfileServer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by danny on 25.01.2016.
 */
public class SettingsFragment extends PreferenceFragment {

    final public static String APP_PREF_NAME = "appsettings";
	private static final String TAG = "SettingsFragment";
	private int tryCount;
	private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
		context = getActivity().getApplicationContext();
		// Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(APP_PREF_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        findPreference(getString(R.string.settings_key_change_password)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, new ChangeBoxAccountPasswordFragment()).addToBackStack(null).commit();
				return true;
			}
		});
		//@todo only for debug. remove in production version
        findPreference(getString(R.string.settings_key_internal_feedback)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                FeedbackManager.register(getActivity(), getString(R.string.hockeykey), null);
                FeedbackManager.showFeedbackActivity(getActivity());
                return true;
            }
        });
		findPreference(getString(R.string.settings_box_quota)).setSummary(R.string.fetching_box_quota);

		loadQuotaInBackground();
	}

	private void loadQuotaInBackground() {

		if (tryCount < 3) {
			new ProfileServer().getProfile(context, new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {

					Log.d(TAG, "Server communication failed: ", e);
					tryCount++;
					loadQuotaInBackground();
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {

					int code = response.code();

					Log.d(TAG, "Server response code: " + response.code());

					if (code == 200) {
						if (parseQuota(response)) {
							return;
						}
					}
					tryCount++;
					loadQuotaInBackground();
				}
			});
		} else {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					findPreference(getString(R.string.settings_box_quota)).setSummary(R.string.cannot_fetch_quota);
				}
			});
		}
	}

	public boolean parseQuota(Response response) throws IOException {
		String text = response.body().string();
		try {
			final ProfileServer.ServerResponse result = ProfileServer.parseJson(new JSONObject(text));
			Log.d(TAG, "quota: " + result.quota);
			Log.d(TAG, "used storage: " + result.usedStorage);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String summary;
					if (result.quota == 0) {
						summary = String.format(getResources().getString(R.string.used_storage),
								FileUtils.byteCountToDisplaySize(result.usedStorage),
								getResources().getString(R.string.unlimited_storage));
					} else {
						summary = String.format(getResources().getString(R.string.used_storage),
								FileUtils.byteCountToDisplaySize(result.usedStorage),
								FileUtils.byteCountToDisplaySize(result.quota));
					}
					findPreference(getString(R.string.settings_box_quota)).setSummary(summary);
				}
			});
		} catch (JSONException e) {
			Log.d(TAG, "Cannot parse JSON " + text);
			return false;
		}
		return true;
	}

}

