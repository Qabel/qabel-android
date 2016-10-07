package de.qabel.qabelbox.communication.callbacks;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.persistence.SimpleJSONAdapter;
import okhttp3.Response;

public abstract class JSONModelCallback<T> extends JsonRequestCallback {

    protected SimpleJSONAdapter<T> jsonAdapter;

    public JSONModelCallback(SimpleJSONAdapter<T> adapter){
        this.jsonAdapter = adapter;
    }

    @Override
    protected void onJSONSuccess(Response response, JSONObject result) {
        try {
            T model = jsonAdapter.fromJson(result);
            onSuccess(response, model);
        } catch (JSONException e) {
            onError(e, response);
        }
    }

    protected abstract void onSuccess(Response response, T model);

}
