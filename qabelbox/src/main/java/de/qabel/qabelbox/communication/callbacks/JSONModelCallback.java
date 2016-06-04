package de.qabel.qabelbox.communication.callbacks;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.persistence.SimpleJSONModel;
import okhttp3.Response;

public abstract class JSONModelCallback<T extends SimpleJSONModel> extends JsonRequestCallback {

    @Override
    protected void onJSONSuccess(Response response, JSONObject result) {
        try {
            T model = createModel();
            model.fromJson(result);
            onSuccess(response, model);
        } catch (JSONException e) {
            onError(e, response);
        }
    }

    protected abstract T createModel();

    protected abstract void onSuccess(Response response, T model);

}
