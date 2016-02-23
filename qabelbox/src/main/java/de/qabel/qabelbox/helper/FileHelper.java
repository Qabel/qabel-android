package de.qabel.qabelbox.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple class to access file actions
 * Created by danny on 04.02.16.
 */
public class FileHelper {

    private static String TAG = "FileHelper";

    public static JSONObject readFileAsJson(FileInputStream fis) throws JSONException, IOException {

        return new JSONObject(readFileAsText(fis));
    }

    public static String loadFileFromAssets(Context c, String file) {

        AssetManager assetManager = c.getAssets();

        InputStream input = null;
        try {
            input = assetManager.open(file);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();
            return new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "can't load file from assets", e);
            return null;
        }
    }

    public static String readFileAsText(FileInputStream fis) throws IOException {

        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[1024];
        int n;
        while ((n = fis.read(buffer)) != -1) {
            fileContent.append(new String(buffer, 0, n));
        }
        return fileContent.toString();
    }
}
