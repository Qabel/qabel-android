package de.qabel.qabelbox.communication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;

/**
 * Class to hold uploadAndDeleteLocalfile/download on separate place
 * Created by danny on 10.02.16.
 */
public class VolumeFileTransferHelper {

    private static final String TAG = "DownloadUploadHelper";
    private static final String URI_PREFIX_FILE = "file://";

    public static Uri getUri(BoxObject boxObject, BoxVolume boxVolume, BoxNavigation boxNavigation) {

        String path = boxNavigation.getPath(boxObject);
        String documentId = boxVolume.getDocumentId(path);
        return DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, documentId);
    }

    public static void upload(final Context self, final Uri uri, final BoxNavigation boxNavigation, final BoxVolume boxVolume) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                String name = getName(self, uri);
                if (name != null) {
                    Uri uploadUri = makeUri(name, boxNavigation, boxVolume);
                    try (InputStream content = self.getContentResolver().openInputStream(uri);
                         OutputStream upload = self.getContentResolver().openOutputStream(uploadUri, "w")) {
                        if (upload == null || content == null) {
                            return null;
                        }
                        IOUtils.copy(content, upload);
                    } catch (IOException e) {
                        Log.e(TAG, "Upload failed", e);
                    }
                }
                return null;
            }
        }.execute();
    }

    private static Uri makeUri(String name, BoxNavigation boxNavigation, BoxVolume boxVolume) {

        String path = boxNavigation.getPath();
        String folderId = boxVolume.getDocumentId(path);
        return DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, folderId + name);
    }

    private static String getName(Context context, Uri uri) {

        String name;
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        if (returnCursor != null) {
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            name = returnCursor.getString(nameIndex);
            returnCursor.close();
        } else if (uri.toString().startsWith(URI_PREFIX_FILE)) {
            File file = new File(uri.toString());
            name = file.getName();
        } else {
            Log.e(TAG, "Cannot handle URI for uploadEncrypted: " + uri.toString());
            return null;
        }
        return name;
    }

    /**
     * get first prefix from identity
     *
     * @param identity
     * @return
     */
    public static String getPrefixFromIdentity(Identity identity) {

        List<String> prefixes = identity.getPrefixes();
        if (prefixes.size() > 0) {
            return prefixes.get(0);
        } else {
            Log.e(TAG, "No prefix in identity with alias " + identity.getAlias());
            return "NULL-PREFIX";
        }
    }
}
