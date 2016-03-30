package de.qabel.qabelbox.services;

import de.qabel.core.drop.DropURL;
import de.qabel.core.http.HTTPResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * LocalQabelServiceTester allows to test sending and receiving DropMessages without a network connection
 * or a drop server. All send messages are locally stored and can be received via getDropMessages with
 * "http://localhost/dropmessages" as the URI.
 */
public class LocalQabelServiceTester extends LocalQabelService {
    ArrayList<byte[]> dropMessages = new ArrayList<>();

    @Override
    HTTPResult<?> dropHTTPsend(DropURL dropURL, byte[] message) {
        HTTPResult<?> httpResult = new HTTPResult<>();
        httpResult.setOk(true);
        httpResult.setResponseCode(200);
        dropMessages.add(message);
        return httpResult;
    }

    @Override
    HTTPResult<Collection<byte[]>> getDropMessages(URI uri, long timestamp) {
        ArrayList<byte[]> mockDropMessages = new ArrayList<>();
        HTTPResult<Collection<byte[]>> result = new HTTPResult<>();

        // URI to test empty drop
        if (uri.toString().equals("http://localhost/empty")) {
            result.setOk(true);
            result.setData(mockDropMessages);
            return result;
            // URI to test drop with previously send DropMessages
        } else if (uri.toString().equals("http://localhost/dropmessages")) {
            result.setOk(true);
            result.setData(dropMessages);
            return result;
            // URI to test error case.
        } else {
            result.setOk(false);
            return result;
        }
    }
}
