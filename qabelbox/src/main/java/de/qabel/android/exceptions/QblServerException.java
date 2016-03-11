package de.qabel.android.exceptions;

import de.qabel.core.exceptions.QblException;

/**
 * This exception denotes an error status recieved from a Qbl-Server
 * Created by r-hold on 18.02.16.
 */
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
