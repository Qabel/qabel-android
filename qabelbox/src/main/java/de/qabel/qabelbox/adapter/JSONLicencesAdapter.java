package de.qabel.qabelbox.adapter;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 01.03.16.
 */
public class JSONLicencesAdapter extends RecyclerView.Adapter<JSONLicencesAdapter.LicenceViewHolder> {

    enum TYPE {
        Header,
        Info
    }

    private static final String JSON_KEY_LICENCENAME = "name";
    private static final java.lang.String JSON_KEY_COMPONENTS_INFO = "info";
    private static final String JSON_KEY_COMPONENTS = "components";
    private static final String JSON_KEY_LICENCESROOT = "licences";

    LayoutInflater inflater;
    Context ctx;
    JSONArray licencesJSON;

    public JSONLicencesAdapter(Context context, JSONObject masterJSON) {
        licencesJSON = masterJSON.optJSONArray(JSON_KEY_LICENCESROOT);
        ctx = context;
        inflater = LayoutInflater.from(ctx);
    }

    @Override
    public LicenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TYPE type = TYPE.values()[viewType];
        switch (type) {
            case Header:
                return new HeaderViewHolder(inflater.inflate(R.layout.header_licence, parent, false));
            case Info:
            default:
                View v = inflater.inflate(R.layout.item_licence, parent, false);
                return new LicenceViewHolder(v);
        }

    }

    @Override
    public void onBindViewHolder(LicenceViewHolder holder, int position) {
        holder.onBind(position);
    }

    @Override
    public int getItemCount() {
        return licencesJSON.length();
    }

    private TYPE getItemType(int position) {
        if (position == 0) {
            return TYPE.Header;
        } else {
            return TYPE.Info;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemType(position).ordinal();
    }

    class LicenceViewHolder extends RecyclerView.ViewHolder {
        public TextView headline;
        public TextView content;

        public LicenceViewHolder(View itemView) {
            super(itemView);
            headline = (TextView) itemView.findViewById(R.id.about_licences_item_headline_txt);
            content = (TextView) itemView.findViewById(R.id.about_licences_item_components_txt);
        }

        public void onBind(int position) {
            try {
                JSONObject licenceJSON = licencesJSON.getJSONObject(position);
                headline.setText(ctx.getString(R.string.about_licence_item_fromhtml, licenceJSON.getString(JSON_KEY_LICENCENAME)));
                String content = licenceJSON.optString(JSON_KEY_COMPONENTS_INFO, "") + "\n";
                JSONArray componentsJSON = licenceJSON.optJSONArray(JSON_KEY_COMPONENTS);
                for (int i = 0; i < componentsJSON.length(); i++) {
                    content += componentsJSON.get(i) + "\n";
                }
                content.trim();
                this.content.setText(content);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class HeaderViewHolder extends LicenceViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
            this.headline = (TextView) itemView.findViewById(R.id.licence_header_versioninfo);
            this.content = (TextView) itemView.findViewById(R.id.licence_header_intro);
        }

        public void onBind(int position) {
            headline.setText(BuildConfig.VERSION_NAME);
        }
    }
}
