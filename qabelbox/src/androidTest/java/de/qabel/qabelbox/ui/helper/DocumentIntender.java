package de.qabel.qabelbox.ui.helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.test.espresso.intent.Intents;

import java.io.File;

import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;

/**
 * Created by danny on 17.03.16.
 */
public class DocumentIntender {


	public void handleAddFileIntent(File file) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			handleAddFileIntentKitKat(file);
		} else {
			handleAddFileIntentOlder(file);
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void handleAddFileIntentKitKat(File file) {
		Intent data = new Intent();
		data.setData(Uri.fromFile(file));

		Intents.intending(allOf(
				hasAction(Intent.ACTION_OPEN_DOCUMENT),
				hasType("*/*"),
				hasCategories(hasItem(Intent.CATEGORY_OPENABLE)),
				hasExtraWithKey(Intent.EXTRA_ALLOW_MULTIPLE)
		)).respondWith(
				new Instrumentation.ActivityResult(Activity.RESULT_OK, data)
		);
	}

	private void handleAddFileIntentOlder(File file) {
		Intent data = new Intent();
		data.setData(Uri.fromFile(file));

		Intents.intending(allOf(
				hasAction(Intent.ACTION_GET_CONTENT),
				hasType("*/*"),
				hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
		)).respondWith(
				new Instrumentation.ActivityResult(Activity.RESULT_OK, data)
		);
	}

}
