package de.qabel.qabelbox;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import org.spongycastle.util.encoders.Hex;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.qabelbox.providers.BoxProvider;

public class QabelBoxApplication extends Application {
    private static final String PREF_DEVICE_ID_CREATED = "PREF_DEVICE_ID_CREATED";
    private static final String PREF_DEVICE_ID = "PREF_DEVICE_ID";
    private static final int NUM_BYTES_DEVICE_ID = 16;
    private static SharedPreferences sharedPreferences;
    public static BoxProvider boxProvider;

    public BoxProvider getProvider() {
        return boxProvider;
    }

    public static byte[] getDeviceID() {
        String deviceID = sharedPreferences.getString(PREF_DEVICE_ID, "");
        if (deviceID.equals("")) {
            // Should never occur
            throw new RuntimeException("DeviceID not created!");
        }
        return Hex.decode(deviceID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(this.getClass().getCanonicalName(), MODE_PRIVATE);

        if (!sharedPreferences.getBoolean(PREF_DEVICE_ID_CREATED, false)) {

            CryptoUtils cryptoUtils = new CryptoUtils();
            byte[] deviceID = cryptoUtils.getRandomBytes(NUM_BYTES_DEVICE_ID);

            Log.d(this.getClass().getName(), "New device ID: " + Hex.toHexString(deviceID));

            sharedPreferences.edit().putString(PREF_DEVICE_ID, Hex.toHexString(deviceID))
                                    .putBoolean(PREF_DEVICE_ID_CREATED, true)
                                    .apply();
        }
    }
}
