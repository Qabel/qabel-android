package de.qabel.qabelbox.adapter;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.ui.views.ButtonFont;

public class JSONLicencesAdapter extends RecyclerView.Adapter<JSONLicencesAdapter.LicenceViewHolder> {

    public static String TAG = "JSONLicencesAdapter";

    enum TYPE {
        Header,
        Info
    }

    private static final String JSON_KEY_LICENCE_NAME = "name";
    private static final String JSON_KEY_COMPONENTS_INFO = "info";
    private static final String JSON_KEY_COMPONENTS = "components";
    private static final String JSON_KEY_LICENCES_ROOT = "licences";
    private static final String JSON_KEY_LICENCE_CONTENT = "content";

    LayoutInflater inflater;
    Context ctx;
    JSONArray licencesJSON;
    String qapl;

    public JSONLicencesAdapter(Context context, JSONObject masterJSON, String qaplText) {
        licencesJSON = masterJSON.optJSONArray(JSON_KEY_LICENCES_ROOT);
        ctx = context;
        inflater = LayoutInflater.from(ctx);
        this.qapl = qaplText;
    }

    @Override
    public LicenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TYPE type = TYPE.values()[viewType];
        switch (type) {
            case Header:
                return new HeaderViewHolder(inflater.inflate(R.layout.header_licence, parent, false), qapl);
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
        return licencesJSON.length() + 1;
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

    protected void showDialog(String header, String content) {
        final SpannableString s = new SpannableString(content);
        Linkify.addLinks(s, Linkify.WEB_URLS);
        AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
        alertDialog.setTitle(header);
        alertDialog.setMessage(s);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, ctx.getText(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
        ((TextView) alertDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    class LicenceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView headline;
        public TextView content;
        public Button showLicenceBtn;
        public JSONObject licenceJSON;
        public String licenceText;
        private String licenceContentText;

        public LicenceViewHolder(View itemView) {
            super(itemView);
            headline = (TextView) itemView.findViewById(R.id.about_licences_item_headline_txt);
            content = (TextView) itemView.findViewById(R.id.about_licences_item_components_txt);
            showLicenceBtn = (ButtonFont) itemView.findViewById(R.id.about_licences_item_showlicence_btn);
        }

        public void onBind(int position) {
            try {
                licenceJSON = licencesJSON.getJSONObject(position - 1);
                headline.setText(licenceJSON.getString(JSON_KEY_LICENCE_NAME));
                String content = licenceJSON.optString(JSON_KEY_COMPONENTS_INFO, "") + "\n";
                JSONArray componentsJSON = licenceJSON.optJSONArray(JSON_KEY_COMPONENTS);
                if (componentsJSON != null) {
                    for (int i = 0; i < componentsJSON.length(); i++) {
                        content += componentsJSON.get(i) + "<br/>";
                    }
                    if (content.length() > 0 && componentsJSON.length() > 0) {
                        content = content.substring(0, content.lastIndexOf("<br/>")).trim(); // Strip last linebreak
                    }
                }
                SpannableString formattedText = new SpannableString(Html.fromHtml(content));
                this.content.setText(formattedText, TextView.BufferType.SPANNABLE);
                this.showLicenceBtn.setOnClickListener(this);
                licenceText = licenceJSON.getString(JSON_KEY_LICENCE_NAME);
                licenceContentText = licenceJSON.getString(JSON_KEY_LICENCE_CONTENT);
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse licences JSON: " + e);
            }
        }

        @Override
        public void onClick(View v) {
            showDialog(licenceText, licenceContentText);
        }
    }


    class HeaderViewHolder extends LicenceViewHolder {

        String qapl;

        public HeaderViewHolder(View itemView, String qapl) {
            super(itemView);
            this.headline = (TextView) itemView.findViewById(R.id.licence_header_versioninfo);
            this.content = (TextView) itemView.findViewById(R.id.licence_header_intro);
            showLicenceBtn = (ButtonFont) itemView.findViewById(R.id.about_header_showlicence_btn);
            this.qapl = qapl;
        }

        public void onBind(int position) {
            headline.setText(BuildConfig.VERSION_NAME);
            this.showLicenceBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            showDialog(ctx.getString(R.string.qapl), qapl);
        }
    }


}
