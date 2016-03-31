package de.qabel.qabelbox.communication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxVolume;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Class to hold uploadAndDeleteLocalfile/download on separate place
 * Created by danny on 10.02.16.
 */
public class VolumeFileTransferHelper {

    private static final String TAG = "DownloadUploadHelper";
    private static final String URI_PREFIX_FILE = "file://";
    public static final String HARDCODED_ROOT = BoxProvider.DOCID_SEPARATOR
            + BoxProvider.PREFIX + BoxProvider.DOCID_SEPARATOR + BoxProvider.PATH_SEP;

    public static Uri getUri(BoxObject boxObject, BoxVolume boxVolume, BoxNavigation boxNavigation) {

        String path = boxNavigation.getPath(boxObject);
        String documentId = boxVolume.getDocumentId(path);
        return DocumentsContract.buildDocumentUri(
                BoxProvider.AUTHORITY, documentId);
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
                BoxProvider.AUTHORITY, folderId + name);
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
            Log.e(TAG, "Cannot handle URI for upload: " + uri.toString());
            return null;
        }
        return name;
    }

    public static boolean uploadUri(Context context, Uri uri, String targetFolder, Identity identity) {

        String name = getName(context, uri);

        if (name != null) {
            String keyIdentifier = identity.getEcPublicKey()
                    .getReadableKeyIdentifier();
            Uri uploadUri = DocumentsContract.buildDocumentUri(
                    BoxProvider.AUTHORITY, keyIdentifier + HARDCODED_ROOT + targetFolder + name);
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uploadUri, "w");
                 InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null || outputStream == null) {
                    return false;
                }
                IOUtils.copy(inputStream, outputStream);
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error opening output stream for upload", e);
            }
            return true;
        }
        return false;
    }

    /**
     * get first prefix from identity
     */
    public static String getPrefixFromIdentity(Identity identity) {

        List<String> prefixes = identity.getPrefixes();
        if (prefixes.size() > 0) {
            return prefixes.get(0);
        } else {
            return HARDCODED_ROOT;
        }
    }
}
