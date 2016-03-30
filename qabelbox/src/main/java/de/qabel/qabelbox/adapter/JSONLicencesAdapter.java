package de.qabel.qabelbox.adapter;


import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.adapter.JSONLicencesAdapter.LicenceViewHolder;
import de.qabel.qabelbox.views.ButtonFont;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONLicencesAdapter extends Adapter<LicenceViewHolder> {

    public static String TAG = "JSONLicencesAdapter";

    enum TYPE {
        Header,
        Info
    }

    private static final String JSON_KEY_LICENCENAME = "name";
    private static final String JSON_KEY_COMPONENTS_INFO = "info";
    private static final String JSON_KEY_COMPONENTS = "components";
    private static final String JSON_KEY_LICENCESROOT = "licences";
    private static final String JSON_KEY_LICENCECONTENT = "content";

    LayoutInflater inflater;
    Context ctx;
    JSONArray licencesJSON;
    String qapl;

    public JSONLicencesAdapter(Context context, JSONObject masterJSON, String qapl) {
        licencesJSON = masterJSON.optJSONArray(JSON_KEY_LICENCESROOT);
        ctx = context;
        inflater = LayoutInflater.from(ctx);
        this.qapl = qapl;
    }

    @Override
    public LicenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TYPE type = TYPE.values()[viewType];
        switch (type) {
            case Header:
                return new HeaderViewHolder(inflater.inflate(layout.header_licence, parent, false), qapl);
            case Info:
            default:
                View v = inflater.inflate(layout.item_licence, parent, false);
                return new LicenceViewHolder(v);
        }

    }

    @Override
    public void onBindViewHolder(LicenceViewHolder holder, int position) {
        holder.onBind(position);

    }

    @Override
    public int getItemCount() {
        int count = licencesJSON.length() + 1;
        return count;
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

    class LicenceViewHolder extends ViewHolder implements View.OnClickListener {
        public TextView headline;
        public TextView content;
        public Button showLicenceBtn;
        public JSONObject licenceJSON;
        public String licenceText;
        private String licenceContentText;

        public LicenceViewHolder(View itemView) {
            super(itemView);
            headline = (TextView) itemView.findViewById(id.about_licences_item_headline_txt);
            content = (TextView) itemView.findViewById(id.about_licences_item_components_txt);
            showLicenceBtn = (ButtonFont) itemView.findViewById(id.about_licences_item_showlicence_btn);
        }

        public void onBind(int position) {
            try {
                licenceJSON = licencesJSON.getJSONObject(position - 1);
                headline.setText(licenceJSON.getString(JSON_KEY_LICENCENAME));
                String content = licenceJSON.optString(JSON_KEY_COMPONENTS_INFO, "") + "\n";
                JSONArray componentsJSON = licenceJSON.optJSONArray(JSON_KEY_COMPONENTS);
                if (componentsJSON != null) {
                    for (int i = 0; i < componentsJSON.length(); i++) {
                        content += componentsJSON.get(i) + "<br/>";
                    }
                    if (content.length() > 0 && componentsJSON.length() > 0) {
                        content = content.substring(0, content.lastIndexOf("<br/>")); // Strip last linebreak
                        content.trim();
                    }
                }
                SpannableString formattedText = new SpannableString(Html.fromHtml(content));
                this.content.setText(formattedText, BufferType.SPANNABLE);
                showLicenceBtn.setOnClickListener(this);
                licenceText = licenceJSON.getString(JSON_KEY_LICENCENAME);
                licenceContentText = licenceJSON.getString(JSON_KEY_LICENCECONTENT);
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse licences JSON: " + e);
            }
        }

        @Override
        public void onClick(View v) {
            AlertDialog alertDialog = new Builder(ctx).create();
            alertDialog.setTitle(licenceText);
            alertDialog.setMessage(licenceContentText);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }


    class HeaderViewHolder extends LicenceViewHolder {

        String qapl;

        public HeaderViewHolder(View itemView, String qapl) {
            super(itemView);
            headline = (TextView) itemView.findViewById(id.licence_header_versioninfo);
            content = (TextView) itemView.findViewById(id.licence_header_intro);
            showLicenceBtn = (ButtonFont) itemView.findViewById(id.about_header_showlicence_btn);
            this.qapl = qapl;
        }

        @Override
        public void onBind(int position) {
            headline.setText(BuildConfig.VERSION_NAME);
            showLicenceBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            AlertDialog alertDialog = new Builder(ctx).create();
            alertDialog.setTitle("QAPL");
            alertDialog.setMessage(qapl);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }


}
