package de.qabel.qabelbox.test.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class FileHelper {

    public static File getSystemTmp() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static String createTestFile() throws IOException {
        File file = createEmptyTargetFile();
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
        File file = createEmptyTargetFile();
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[]{1, 2, 3, 4, 5};
        outputStream.write(testData);
        outputStream.close();
        return file;
    }

    public static File createEmptyTargetFile() throws IOException {
        return File.createTempFile("test", null, getSystemTmp());
    }

    public static File createTestFile(long kb) throws Exception {
        File testFile = createEmptyTargetFile();
        OutputStream outputStream = new FileOutputStream(testFile);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < kb; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return testFile;
    }

    public static File createTmpDir(){
        File tmpDir = new File(getSystemTmp(), UUID.randomUUID().toString());
        if(!tmpDir.mkdirs()){
            throw new RuntimeException("Cannot create tmp directory");
        }
        return tmpDir;
    }

}
