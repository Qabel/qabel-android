package de.qabel.qabelbox.storage.data;

import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.persistence.SimpleJSONAdapter;
import de.qabel.qabelbox.storage.model.BoxQuota;

public class BoxQuotaJSONAdapter extends SimpleJSONAdapter<BoxQuota> {

    private static final String KEY_SIZE = "size";
    private static final String KEY_QUOTA = "quota";

    @Override
    protected BoxQuota createModel() {
        return new BoxQuota();
    }

    @Override
    protected void readJson(JSONObject o, BoxQuota model) {
        model.setSize(o.optLong(KEY_SIZE, 0));
        model.setQuota(o.optLong(KEY_QUOTA, 0));
    }

    @Override
    protected void writeJson(BoxQuota model, JSONObject o) throws JSONException {
        o.put(KEY_SIZE, model.getSize());
        o.put(KEY_QUOTA, model.getQuota());
    }
}
