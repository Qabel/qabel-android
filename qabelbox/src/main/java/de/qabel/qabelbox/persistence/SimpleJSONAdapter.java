package de.qabel.qabelbox.persistence;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SimpleJSONAdapter<T> {

    protected abstract T createModel();

    protected abstract void readJson(JSONObject o, T model);

    protected abstract void writeJson(T model, JSONObject o) throws JSONException;

    public JSONObject toJson(T model) throws JSONException {
        JSONObject data = new JSONObject();
        writeJson(model, data);
        return data;
    }

    public T fromJson(JSONObject data) throws JSONException {
        T model = createModel();
        readJson(data, model);
        return model;
    }
}
