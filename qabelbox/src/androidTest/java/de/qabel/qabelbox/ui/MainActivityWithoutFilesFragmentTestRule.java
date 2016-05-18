package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import de.qabel.qabelbox.activities.MainActivity;

class MainActivityWithoutFilesFragmentTestRule extends IntentsTestRule<MainActivity> {

    public MainActivityWithoutFilesFragmentTestRule(boolean launchActivity) {
        super(MainActivity.class, true, launchActivity);
    }

    public MainActivityWithoutFilesFragmentTestRule() {
        super(MainActivity.class, true, false);
    }

    @Override
    protected void afterActivityLaunched() {
        try {
            super.afterActivityLaunched();
        } catch (IllegalStateException ignored) {
            // Sometimes Intents.init was called twice because of an error in the previous test.
        }
    }

    @Override
    protected Intent getActivityIntent() {
        Intent intent = super.getActivityIntent();
        intent.putExtra(MainActivity.START_FILES_FRAGMENT, false);
        return intent;
    }
}
