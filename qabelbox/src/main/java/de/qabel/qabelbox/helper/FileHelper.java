package de.qabel.qabelbox.helper;


import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Simple class to access file actions
 * Created by danny on 04.02.16.
 */
public class FileHelper {

	private static final String DEFAULT_ENCODING = "UTF-8";
	public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);

	private static String TAG = "FileHelper";

	public static JSONObject readFileAsJson(InputStream fis) throws JSONException, IOException {

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

	public static String readFileAsText(InputStream fis) throws IOException {

		StringBuffer fileContent = new StringBuffer("");

		byte[] buffer = new byte[4096];
		int n;
		while ((n = fis.read(buffer)) != -1) {
			fileContent.append(new String(buffer, 0, n));
		}
		return fileContent.toString();
	}

	public static byte[] readFileAsBinary(FileInputStream fis) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int n;
		while ((n = fis.read(buffer)) != -1) {
			baos.write(buffer, 0, n);

		}
		return baos.toByteArray();
	}

	private static final int BUFFER_SIZE = 1024 * 4;

	/**
	 * Reads and returns the rest of the given input stream as a byte array,
	 * closing the input stream afterwards.
	 */
	public static byte[] toByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			byte[] b = new byte[BUFFER_SIZE];
			int n = 0;
			while ((n = is.read(b)) != -1) {
				output.write(b, 0, n);
			}
			return output.toByteArray();
		} finally {
			output.close();
		}
	}

	/**
	 * Reads and returns the rest of the given input stream as a string, closing
	 * the input stream afterwards.
	 */
	public static String toString(InputStream is) throws IOException {
		return new String(toByteArray(is), UTF8);
	}

	public static String getRealPathFromURI(Context context, Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};
		CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		if (cursor == null) {
			return null;
		}
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String result = cursor.getString(column_index);
		cursor.close();
		return result;
	}
}
