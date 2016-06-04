package de.qabel.qabelbox.persistence;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SimpleJSONModel {

    protected abstract void readJson(JSONObject o);

    protected abstract void writeJson(JSONObject o) throws JSONException;

    public JSONObject toJson() throws JSONException {
        JSONObject data = new JSONObject();
        writeJson(data);
        return data;
    }

    public void fromJson(JSONObject data) throws JSONException {
        readJson(data);
    }


}
