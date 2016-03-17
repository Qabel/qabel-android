package de.qabel.qabelbox.ui.helper;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.intent.Intents;

import java.io.File;

import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;

/**
 * Created by danny on 17.03.16.
 */
public class DocumentIntents {

	public void handleSaveFileIntent(File file) {

		Intent data = new Intent();
		data.setData(Uri.fromFile(file));

/*
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/json");
		intent.putExtra(Intent.EXTRA_TITLE, type + "" + filename + "." + QabelSchema.FILE_SUFFIX_CONTACT);*/


		Intents.intending(allOf(
				hasAction(Intent.ACTION_CREATE_DOCUMENT),
				//hasType("application/json"),
				hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
		)).respondWith(
				new Instrumentation.ActivityResult(Activity.RESULT_OK, data)
		);

	}
}
