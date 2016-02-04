package de.qabel.qabelbox.helper;

import android.app.Activity;

import com.google.zxing.integration.android.IntentIntegrator;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;

/**
 * Created by danny on 04.02.16.
 */
public class QRCodeHelper {
    public static void exportIdentityAsContactWithQR(Activity activity,Identity identity) {

        if (identity != null) {
            IntentIntegrator intentIntegrator = new IntentIntegrator(activity);

            intentIntegrator.setTitleByID(R.string.qrcode);
            intentIntegrator.shareText("QABELCONTACT\n"
                    + identity.getAlias() + "\n"
                    + identity.getDropUrls().toArray()[0].toString() + "\n"
                    + identity.getKeyIdentifier());
        }
    }
}
