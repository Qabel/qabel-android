package de.qabel.qabelbox.storage.model;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.persistence.SimpleJSONModel;

public class BoxQuota extends SimpleJSONModel {

    private static final String KEY_SIZE = "size";
    private static final String KEY_QUOTA = "quota";


    private long size = 0;
    private long quota = -1;

    public long getSize() {
        return size;
    }

    public long getQuota() {
        return quota;
    }

    @Override
    protected void readJson(JSONObject o) {
        this.size = o.optLong(KEY_SIZE, 0);
        this.quota = o.optLong(KEY_QUOTA, 0);
    }

    @Override
    protected void writeJson(JSONObject o) throws JSONException {
        o.put(KEY_SIZE, size);
        o.put(KEY_QUOTA, quota);
    }
}
