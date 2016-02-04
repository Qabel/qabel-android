package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import com.google.zxing.qrcode.QRCodeWriter;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;

/**
 * Created by danny on 04.02.16.
 */
public class QRCodeHelper {


    public static Bitmap generateQRCode(Activity activity,Identity identity) {

        String text = "QABELCONTACT\n"
                + identity.getAlias() + "\n"
                + identity.getDropUrls().toArray()[0].toString() + "\n"
                + identity.getKeyIdentifier();

        int size=512;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}
