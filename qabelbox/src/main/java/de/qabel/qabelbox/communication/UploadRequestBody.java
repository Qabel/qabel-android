package de.qabel.qabelbox.communication;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class UploadRequestBody extends RequestBody {

    private InputStream inputStream;
    private MediaType contentType;

    public UploadRequestBody(InputStream inputStream, MediaType contentType) {
        this.inputStream = inputStream;
        this.contentType = contentType;
    }

    @Override
    public long contentLength() {
        try {
            return inputStream.available();
        } catch (Throwable ex) {
            ex.printStackTrace();
            return 0L;
        }
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(inputStream);
            sink.writeAll(source);
        } finally {
            Util.closeQuietly(source);
        }
    }
}
