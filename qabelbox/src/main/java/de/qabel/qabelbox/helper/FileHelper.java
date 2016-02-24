package de.qabel.qabelbox.helper;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Simple class to access file actions
 * Created by danny on 04.02.16.
 */
public class FileHelper {

    public static String readFileAsText(FileInputStream fis) throws IOException {

        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[4096];
        int n;
        while ((n = fis.read(buffer)) != -1) {
            fileContent.append(new String(buffer, 0, n));
        }
        return fileContent.toString();
    }
}
