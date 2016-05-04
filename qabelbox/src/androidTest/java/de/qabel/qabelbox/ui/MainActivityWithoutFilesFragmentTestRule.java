package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import de.qabel.qabelbox.activities.MainActivity;

class MainActivityWithoutFilesFragmentTestRule extends IntentsTestRule<MainActivity> {

    public MainActivityWithoutFilesFragmentTestRule(boolean launchActivtiy) {
        super(MainActivity.class, true, launchActivtiy);
    }

    public MainActivityWithoutFilesFragmentTestRule() {
        super(MainActivity.class, true, false);
    }

    @Override
    protected Intent getActivityIntent() {
        Intent intent = super.getActivityIntent();
        intent.putExtra(MainActivity.START_FILES_FRAGMENT, false);
        return intent;
    }
}
