package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.color;
import de.qabel.qabelbox.R.dimen;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.WelcomeScreenActivity;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.helper.UIHelper;

public class WelcomeDisclaimerFragment extends Fragment {
    private WelcomeScreenActivity mActivity;
    private CheckBox cbLegal;
    private CheckBox cbPrivacy;

    enum Type {QAPL, PRIVACY, LEGAL}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof WelcomeScreenActivity) {
            mActivity = (WelcomeScreenActivity) activity;
        } else {
            new Throwable("fragment can't attach to non WelcomeScreenActivity");
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(layout.fragment_welcome_disclaimer, container, false);

        setShader((TextView) view.findViewById(id.welcome_text2));
        setShader((TextView) view.findViewById(id.welcome_text4));
        setShader((TextView) view.findViewById(id.welcome_text5));
        setShader((TextView) view.findViewById(id.headline1));
        setShader((TextView) view.findViewById(id.headline2));

        cbLegal = (CheckBox) view.findViewById(id.cb_welcome_legal);
        cbPrivacy = (CheckBox) view.findViewById(id.cb_welcome_privacy);
        setSmallShader(cbLegal);
        setSmallShader(cbPrivacy);

        setClickListeners(view);
        return view;
    }

    private void setClickListeners(View view) {
        cbPrivacy.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateRightButtonTextColor(isChecked && cbLegal.isChecked());
                if (isChecked) {
                    cbPrivacy.setText(string.cb_welcome_disclaimer_privacy_note_checked);
                } else {
                    cbPrivacy.setText(string.cb_welcome_disclaimer_privacy_note_unchecked);
                }
            }
        });
        cbLegal.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateRightButtonTextColor(isChecked && cbPrivacy.isChecked());
                if (isChecked) {
                    cbLegal.setText(string.cb_welcome_disclaimer_legal_note_checked);
                } else {
                    cbLegal.setText(string.cb_welcome_disclaimer_legal_note_unchecked);
                }
            }
        });
        setClickListener(view, id.btn_show_qapl, Type.QAPL);
        setClickListener(view, id.btn_show_privacy, Type.PRIVACY);
        setClickListener(view, id.btn_show_legal, Type.LEGAL);


    }

    private void setClickListener(View fragmentView, int view, final Type mode) {

        fragmentView.findViewById(view).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                WebView webView = new WebView(getActivity());
                String file = "";
                if (mode == Type.QAPL) {
                    file = getResources().getString(string.FILE_QAPL_LICENSE);
                }
                if (mode == Type.PRIVACY) {
                    file = getResources().getString(string.FILE_PRIVACY);
                }
                if (mode == Type.LEGAL) {
                    file = getResources().getString(string.FILE_LEGAL);
                }

                webView.loadDataWithBaseURL("file:///android_asset/", FileHelper.loadFileFromAssets(getActivity(), "html/help/" + file),
                        "text/html", "utf-8", null);

                UIHelper.showCustomDialog(getActivity(), webView, string.ok, null);
            }
        });
    }

    private void updateRightButtonTextColor(boolean allChecked) {

        if (allChecked) {
            mActivity.setRightButtonColor(getResources().getColor(color.welcome_button_activated));
        } else {
            mActivity.setRightButtonColor(getResources().getColor(color.welcome_button_deactivated));
        }
    }

    public boolean getCheckedState() {
        return cbLegal.isChecked() && cbPrivacy.isChecked();
    }

    private void setShader(TextView tv) {
        float dx = getResources().getDimension(dimen.welcome_shadow_dx);
        float dy = getResources().getDimension(dimen.welcome_shadow_dy);
        float radius = getResources().getDimension(dimen.welcome_shadow_radius);
        int col = getResources().getColor(color.welcome_shadow);
        tv.setShadowLayer(radius, dx, dy, col);
    }

    private void setSmallShader(TextView tv) {
        float dx = getResources().getDimension(dimen.welcome_shadow_dx);
        float dy = getResources().getDimension(dimen.welcome_shadow_dy);
        float radius = getResources().getDimension(dimen.welcome_shadow_radius);
        int col = getResources().getColor(color.welcome_shadow);
        tv.setShadowLayer(radius, dx, dy, col);
    }
}
