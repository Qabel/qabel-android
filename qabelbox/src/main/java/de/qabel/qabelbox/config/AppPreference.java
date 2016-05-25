package de.qabel.qabelbox.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.spongycastle.util.encoders.Hex;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.qabelbox.services.LocalQabelService;

public class AppPreference {

    private static final int NUM_BYTES_DEVICE_ID = 16;

    private static final String P_DEVICE_ID = "PREF_DEVICE_ID";
    private static final String P_TOKEN = "token";
    private static final String P_ACCOUNT_NAME = "boxaccount";
    private static final String P_ACCOUNT_EMAIL = "boxemail";
    private static final String P_LAST_APP_START_VERSION = "lastappstartversion";
    private static final String P_WELCOME_SCREEN_SHOWN_AT = "welcomescreenshownat";

    private final Context context;
    private final SharedPreferences settings;

    public AppPreference(Context context) {
        this.context = context;
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public void clear() {
        settings.edit().clear().commit();
    }

    public void setToken(String token) {
        settings.edit().putString(P_TOKEN, token).commit();
    }

    public String getToken() {
        return settings.getString(P_TOKEN, null);
    }

    public void setAccountName(String name) {
        settings.edit().putString(P_ACCOUNT_NAME, name).commit();
    }

    public String getAccountName() {

        return settings.getString(P_ACCOUNT_NAME, null);
    }

    public void setAccountEMail(String email) {
        settings.edit().putString(P_ACCOUNT_EMAIL, email).commit();
    }

    public String getAccountEMail() {
        return settings.getString(P_ACCOUNT_EMAIL, null);
    }

    public int getLastAppStartVersion() {
        return settings.getInt(P_LAST_APP_START_VERSION, 0);
    }

    public void setLastAppStartVersion(int version) {
        settings.edit().putInt(P_LAST_APP_START_VERSION, version).commit();
    }

    public long getWelcomeScreenShownAt() {
        return settings.getLong(P_WELCOME_SCREEN_SHOWN_AT, 0);
    }

    public void setWelcomeScreenShownAt(long time) {
        settings.edit().putLong(P_WELCOME_SCREEN_SHOWN_AT, time).commit();
    }

    public void logout() {
        setToken(null);
    }

    public byte[] getDeviceId() {
        String deviceID = settings.getString(P_DEVICE_ID, null);
        if (deviceID == null) {
            deviceID = getLegacyDeviceId();
            if (deviceID == null) {
                CryptoUtils cryptoUtils = new CryptoUtils();
                byte[] deviceIDBytes = cryptoUtils.getRandomBytes(NUM_BYTES_DEVICE_ID);
                deviceID = Hex.toHexString(deviceIDBytes);
            }
            settings.edit().putString(P_DEVICE_ID, deviceID);
        }
        return Hex.decode(deviceID);
    }

    private String getLegacyDeviceId() {
        SharedPreferences oldPrefs = context.getSharedPreferences(LocalQabelService.class.getCanonicalName(), Context.MODE_PRIVATE);
        return oldPrefs.getString(P_DEVICE_ID, null);
    }
}
