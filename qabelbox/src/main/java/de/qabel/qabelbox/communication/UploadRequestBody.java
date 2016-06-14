package de.qabel.qabelbox.communication;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

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
        Subscription progressSubscription = null;
        try {
            source = Okio.source(file);
            final long total[] = new long[]{0};
            long read;
            Observable<Long> observable = Observable.interval(250, TimeUnit.MILLISECONDS);
            progressSubscription = observable.subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    listener.onProgress(total[0], file.length());
                }
            });

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total[0] += read;
                sink.flush();
            }
        } finally {
            if (progressSubscription != null) {
                progressSubscription.unsubscribe();
            }
            Util.closeQuietly(source);
        }
    }
}
