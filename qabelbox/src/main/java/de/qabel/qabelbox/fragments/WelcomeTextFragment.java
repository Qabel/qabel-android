package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 23.02.16.
 */
public class WelcomeTextFragment extends Fragment {

	private static final String KEY_MESSAGE_ID = "messageid";
	private static final String KEY_TEXT_BEFORE_QABEL = "before";
	private static final String KEY_TEXT_AFTER_QABEL = "after";

	public static Fragment newInstance(TextElement texts) {
		WelcomeTextFragment fragment = new WelcomeTextFragment();
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_MESSAGE_ID, texts.messageId);
		bundle.putInt(KEY_TEXT_BEFORE_QABEL, texts.textBeforeQabelName);
		bundle.putInt(KEY_TEXT_AFTER_QABEL, texts.textAfterQabelName);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_welcome_text, container, false);
		TextView message = ((TextView) view.findViewById(R.id.welcome_message));
		TextView before = ((TextView) view.findViewById(R.id.welcome_message_headline_before));
		TextView qabel = ((TextView) view.findViewById(R.id.welcome_message_headline_qabel));
		TextView after = ((TextView) view.findViewById(R.id.welcome_message_headline_afer));
		message.setText(getArguments().getInt(KEY_MESSAGE_ID));
		before.setText(getArguments().getInt(KEY_TEXT_BEFORE_QABEL));
		after.setText(getArguments().getInt(KEY_TEXT_AFTER_QABEL));
		setShader(message);
		setShader(before);
		setShader(qabel);
		setShader(after);
		return view;
	}

	private void setShader(TextView tv) {
		float dx = getResources().getDimension(R.dimen.welcome_shadow_dx);
		float dy = getResources().getDimension(R.dimen.welcome_shadow_dy);
		float radius = getResources().getDimension(R.dimen.welcome_shadow_radius);
		int col = getResources().getColor(R.color.welcome_shadow);
		tv.setShadowLayer(radius, dx, dy, col);
	}

	public static class TextElement {
		final int messageId;
		final int textBeforeQabelName;
		final int textAfterQabelName;

		/**
		 * create new text element
		 *
		 * @param message message id
		 * @param before  text before QABEL
		 * @param after   text after QABEL
		 */
		public TextElement(int message, int before, int after) {
			this.messageId = message;
			this.textBeforeQabelName = before;
			this.textAfterQabelName = after;
		}
	}
}
