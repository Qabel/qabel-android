package de.qabel.qabelbox.exceptions;

import de.qabel.core.exceptions.QblException;

public class QblServerException extends QblException {
    public int getStatusCode() {
        return statusCode;
    }

    public String getServerCall() {
        return serverCall;
    }

    int statusCode;
    String serverCall;

    public QblServerException(int statusCode, String serverCall) {
        super("StatusCode " + statusCode + " recieved for " + serverCall);
        this.statusCode = statusCode;
        this.serverCall = serverCall;
    }
}
