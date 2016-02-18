package de.qabel.qabelbox.chat;

import android.util.Log;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.qabel.qabelbox.helper.FileHelper;

/**
 * Created by danny on 17.02.16.
 */
public class ChatStreamParser extends AbstractContentHandler {

    final ArrayList<byte[]> parts = new ArrayList<>();
    private static final String TAG = "ChatStreamParser";

    @Override
    public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {

        Log.d(TAG, "body " + bd.toString());
        byte[] data = FileHelper.readInputStreamAsData(is);
        parts.add(data);
    }
}
