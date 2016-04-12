package de.qabel.qabelbox.communication;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;

public class RequestAction {

    private Request request;
    private Callback callback;
    private Call call;

    public RequestAction(Request request, Callback callback) {
        this.request = request;
        this.callback = callback;
    }

    public Request getRequest() {
        return request;
    }

    public Callback getCallback() {
        return callback;
    }

    public Call getCall() {
        return call;
    }

    public void setCall(Call call) {
        System.out.println("CALL EXECUTED: " + call.request().toString());
        this.call = call;
    }

    public boolean isExecuted() {
        return this.call != null && this.call.isExecuted();
    }

    public boolean isCanceled() {
        return this.call != null && this.call.isCanceled();
    }
}
