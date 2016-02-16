package de.qabel.qabelbox.communication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxVolume;

/**
 * Class to hold upload/download on separate place
 * Created by danny on 10.02.16.
 */
public class VolumeFileTransferHelper {

    private static final String TAG = "DownloadUploadHelper";
    public static final String HARDCODED_ROOT = BoxProvider.DOCID_SEPARATOR
            + BoxProvider.BUCKET + BoxProvider.DOCID_SEPARATOR
            + BoxProvider.PREFIX + BoxProvider.DOCID_SEPARATOR + BoxProvider.PATH_SEP;

    public static void upload(final Context self, final Uri uri, final BoxNavigation boxNavigation, final BoxVolume boxVolume) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                Cursor returnCursor =
                        self.getContentResolver().query(uri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String name = returnCursor.getString(nameIndex);
                returnCursor.close();

                try {
                    Uri uploadUri = makeUri(name, boxNavigation, boxVolume);

                    InputStream content = self.getContentResolver().openInputStream(uri);
                    OutputStream upload = self.getContentResolver().openOutputStream(uploadUri, "w");
                    if (upload == null || content == null) {
                        return null;
                    }
                    IOUtils.copy(content, upload);
                    content.close();
                    upload.close();
                } catch (IOException e) {
                    Log.e(TAG, "Upload failed", e);
                }
                return null;
            }
        }.execute();
    }

    private static Uri makeUri(String name, BoxNavigation boxNavigation, BoxVolume boxVolume) {

        String path = boxNavigation.getPath();
        String folderId = boxVolume.getDocumentId(path);
        return DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, folderId + name);
    }

    public static boolean uploadUri(Context context, Uri uri, String targetFolder, Identity identity) {

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "No valid url for uploading" + uri);
            return true;
        }
        cursor.moveToFirst();
        String displayName = cursor.getString(
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        Log.i(TAG, "Displayname: " + displayName);
        String keyIdentifier = identity.getEcPublicKey()
                .getReadableKeyIdentifier();
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, keyIdentifier + HARDCODED_ROOT + targetFolder + displayName);
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uploadUri, "w");
            if (outputStream == null) {
                return false;
            }
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error opening output stream for upload", e);
        }
        return false;
    }
}
