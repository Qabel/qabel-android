package de.qabel.qabelbox.test.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileHelper {

    public static File getSystemTmp() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static String createTestFile() throws IOException {
        File tmpDir = getSystemTmp();
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < 100; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return file.getAbsolutePath();
    }

    public static File smallTestFile() throws IOException {
        File tmpDir = getSystemTmp();
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[]{1, 2, 3, 4, 5};
        outputStream.write(testData);
        outputStream.close();
        return file;
    }

}
