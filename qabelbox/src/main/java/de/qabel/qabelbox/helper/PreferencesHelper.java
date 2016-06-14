package de.qabel.qabelbox.helper;

import android.content.SharedPreferences;

import de.qabel.qabelbox.services.LocalQabelService;

public class PreferencesHelper {
    public static void setLastActiveIdentityID(
            SharedPreferences sharedPreferences, String identityID) {
        sharedPreferences.edit()
                .putString(LocalQabelService.PREF_LAST_ACTIVE_IDENTITY, identityID)
                .apply();
    }
}
