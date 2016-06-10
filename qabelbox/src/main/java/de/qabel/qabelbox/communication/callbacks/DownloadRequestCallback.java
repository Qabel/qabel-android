package de.qabel.qabelbox.communication.callbacks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

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
        Subscription progressSubscription = null;
        try {

            InputStream is = response.body().byteStream();
            BufferedInputStream input = new BufferedInputStream(is);
            OutputStream output = new FileOutputStream(outputFile);

            final byte[] data = new byte[SEGMENT_SIZE];
            final long contentLength = response.body().contentLength();
            final long total[] = new long[]{0};
            int count;
            Observable<Long> observable = Observable.interval(250, TimeUnit.MILLISECONDS);
            progressSubscription = observable.subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    DownloadRequestCallback.this.onProgress(total[0], contentLength);
                }
            });
            while ((count = input.read(data)) != -1) {
                total[0] += count;
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            this.onError(e, null);
        } finally {
            if (progressSubscription != null) {
                progressSubscription.unsubscribe();
            }
        }
    }
}
