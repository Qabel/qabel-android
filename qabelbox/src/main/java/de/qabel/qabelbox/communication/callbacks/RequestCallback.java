package de.qabel.qabelbox.communication.callbacks;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.qabel.qabelbox.exceptions.QblServerException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class RequestCallback implements Callback {

    private static final int DEFAULT_SUCCESS_STATUS_CODE = 200;

    public interface SystemHandler {

        void onRequestError();
        void onRequestSuccess();

    }

    private Set<Integer> acceptedStatusCodes = new HashSet<>();
    private SystemHandler systemHandler;

    public RequestCallback() {
        this(new int[]{DEFAULT_SUCCESS_STATUS_CODE});
    }

    public RequestCallback(int[] acceptedStatusCodes) {
        for (int code : acceptedStatusCodes) {
            this.acceptedStatusCodes.add(code);
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        if (this.systemHandler != null) {
            systemHandler.onRequestError();
        }
        onError(e, null);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        int statusCode = response.code();
        if (acceptedStatusCodes.contains(statusCode)) {
            if (this.systemHandler != null) {
                systemHandler.onRequestSuccess();
            }
            onSuccess(statusCode, response);
        } else {
            onError(new QblServerException(statusCode, call.request().toString()), response);
        }
    }

    public void setSystemHandler(SystemHandler systemHandler) {
        this.systemHandler = systemHandler;
    }

    protected abstract void onSuccess(int statusCode, Response response);

    protected abstract void onError(Exception e, @Nullable Response response);

}
