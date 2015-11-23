package de.qabel.qabelbox.filesystem;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class BoxContentObserver extends ContentObserver {
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public BoxContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.wtf("asd", "onchange");
    }
    @Override

    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        Log.wtf("asd", "onchange 2");

    }
}
