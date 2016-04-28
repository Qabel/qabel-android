package de.qabel.qabelbox.communication;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;

public class RequestAction {

    private int autoRetry = 5;
    private int executed = 0;
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
        this.call = call;
        this.executed++;
    }

    public boolean isExecuted() {
        return this.call != null && this.call.isExecuted();
    }

    public boolean isCanceled() {
        return this.call != null && this.call.isCanceled();
    }

    public int getAutoRetry() {
        return autoRetry;
    }

    public int getExecuted(){
        return executed;
    }
}
