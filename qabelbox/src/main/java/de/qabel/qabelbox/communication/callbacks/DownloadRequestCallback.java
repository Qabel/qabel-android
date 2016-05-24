package de.qabel.qabelbox.communication.callbacks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Response;

public abstract class DownloadRequestCallback extends RequestCallback {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private File outputFile;

    public DownloadRequestCallback(File outputFile) {
        super();
        this.outputFile = outputFile;
    }

    protected void onProgress(long current, long size) {
        //Empty body for not tracked downloads
    }

    @Override
    protected void onSuccess(int statusCode, Response response) {
        try {
            InputStream is = response.body().byteStream();
            BufferedInputStream input = new BufferedInputStream(is);
            OutputStream output = new FileOutputStream(outputFile);

            final byte[] data = new byte[SEGMENT_SIZE];
            long contentLength = response.body().contentLength();
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                onProgress(total, contentLength);
            }
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            this.onError(e, null);
        }
    }
}
