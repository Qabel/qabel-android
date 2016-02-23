package de.qabel.qabelbox.helper;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple class to access file actions
 * Created by danny on 04.02.16.
 */
public class FileHelper {

    public static JSONObject readFileAsJson(FileInputStream fis) throws JSONException, IOException {

        return new JSONObject(readFileAsText(fis));
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
    public static String readInputStreamAsText(InputStream fis) throws IOException {

        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[1024];
        int n;
        while ((n = fis.read(buffer)) != -1) {
            fileContent.append(new String(buffer, 0, n));
        }
        return fileContent.toString();
    }
    public static byte[] readInputStreamAsData(InputStream fis) throws IOException {


        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = fis.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
            Log.d("t", "readed "+n);
        }

        return baos.toByteArray();
    }
}
