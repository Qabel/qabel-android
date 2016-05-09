package de.qabel.qabelbox.ui.helper;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import static android.app.Instrumentation.*;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;

public class DocumentIntents {

    public static void registerFileSaveIntent(File file) {

        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        intending(allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );

    }

    public static void handleLoadFileIntent(File file) {

        Intent data = new Intent();
        data.setData(Uri.fromFile(file));

        intending(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasCategories(hasItem(Intent.CATEGORY_OPENABLE))
        )).respondWith(
                new ActivityResult(Activity.RESULT_OK, data)
        );

    }

}
