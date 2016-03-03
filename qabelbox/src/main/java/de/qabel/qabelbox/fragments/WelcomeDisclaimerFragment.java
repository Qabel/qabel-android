package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.WelcomeScreenActivity;

/**
 * Created by danny on 23.02.16.
 */
public class WelcomeDisclaimerFragment extends Fragment {
	private WelcomeScreenActivity mActivity;
	private CheckBox cbLegal;
	private CheckBox cbPrivacy;

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

		View view = inflater.inflate(R.layout.fragment_welcome_disclaimer, container, false);

		setShader((TextView) view.findViewById(R.id.welcome_text2));
		setShader((TextView) view.findViewById(R.id.welcome_text3));
		setShader((TextView) view.findViewById(R.id.welcome_text4));
		setShader((TextView) view.findViewById(R.id.welcome_text5));
		setShader((TextView) view.findViewById(R.id.headline1));
		setShader((TextView) view.findViewById(R.id.headline2));

		cbLegal = (CheckBox) view.findViewById(R.id.cb_welcome_legal);
		cbPrivacy = (CheckBox) view.findViewById(R.id.cb_welcome_privacy);
		setSmallShader(cbLegal);
		setSmallShader(cbPrivacy);

		setClickListeners();
		return view;
	}

	private void setClickListeners() {
		cbPrivacy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateRightButtonTextColor(isChecked && cbLegal.isChecked());
			}
		});
		cbLegal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateRightButtonTextColor(isChecked && cbPrivacy.isChecked());
			}
		});
	}

	private void updateRightButtonTextColor(boolean allChecked) {

		if (allChecked) {
			mActivity.setRightButtonColor(getResources().getColor(R.color.welcome_button_activated));
		} else {
			mActivity.setRightButtonColor(getResources().getColor(R.color.welcome_button_deactivated));
		}
	}

	public boolean getCheckedState() {
		return cbLegal.isChecked() && cbPrivacy.isChecked();
	}

	private void setShader(TextView tv) {
		float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
		float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
		float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
		int col = getResources().getColor(R.color.welcome_shadow);
		tv.setShadowLayer(radius, dx, dy, col);
	}

	private void setSmallShader(TextView tv) {
		float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
		float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
		float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
		int col = getResources().getColor(R.color.welcome_shadow);
		tv.setShadowLayer(radius, dx, dy, col);
	}
}
