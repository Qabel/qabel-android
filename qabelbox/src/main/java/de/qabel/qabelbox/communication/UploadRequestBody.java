package de.qabel.qabelbox.communication;

import java.io.File;
import java.io.IOException;

import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class UploadRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final File file;
    private final UploadRequestCallback listener;
    private MediaType contentType;

    public UploadRequestBody(File file, MediaType contentType, UploadRequestCallback listener) {
        this.file = file;
        this.contentType = contentType;
        this.listener = listener;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            long total = 0;
            long read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                this.listener.onProgress(total, contentLength());

            }
        } finally {
            Util.closeQuietly(source);
        }
    }
}
