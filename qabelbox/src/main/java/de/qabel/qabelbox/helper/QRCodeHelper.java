package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.qabel.core.config.Identity;

public class QRCodeHelper {
    private static final String TAG = "QRCodeHelper";

    public static void generateQRCode(final Activity activity, final Identity identity, final ImageView iv) {
        new AsyncTask<Identity, Void, Bitmap>() {
            public int size;

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null && iv != null && iv.isAttachedToWindow()) {
                    iv.setImageBitmap(bitmap);
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                iv.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY);
                size = iv.getMeasuredWidth();
                DisplayMetrics metrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                size = Math.max(320, Math.min(1024, metrics.widthPixels));
                Log.d(TAG, "QRCode size " + size);
            }

            @Override
            protected Bitmap doInBackground(Identity... identities) {
                String text = "QABELCONTACT\n"
                        + identities[0].getAlias() + "\n"
                        + identities[0].getDropUrls().toArray()[0] + "\n"
                        + identities[0].getKeyIdentifier();

                QRCodeWriter writer = new QRCodeWriter();
                try {
                    BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
                    int width = bitMatrix.getWidth();
                    int height = bitMatrix.getHeight();
                    BitArray row = new BitArray(width);
                    int[] data = new int[width * height];
                    for (int y = 0; y < height; y++) {
                        int yy = y * width;
                        bitMatrix.getRow(y, row);
                        int[] rowArray = row.getBitArray();
                        for (int x = 0; x < width; x++) {
                            data[x + yy] = (rowArray[x / 32] >>> (x & 0x1f) & 1) != 0 ? Color.BLACK : Color.WHITE;
                        }
                    }

                    return Bitmap.createBitmap(data, 0, width, width, height, Config.RGB_565);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, identity);
    }
}
