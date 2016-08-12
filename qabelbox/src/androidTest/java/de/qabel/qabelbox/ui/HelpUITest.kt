package de.qabel.qabelbox.ui

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withText
import de.qabel.qabelbox.R
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher
import org.junit.Ignore
import org.junit.Test

class HelpUITest : AbstractUITest(){

    @Ignore("Drawer layout rebuild")
    @Test
    fun testShowHelp() {
        launchActivity(null)
        //DrawerActions.openDrawer(R.id.drawer_layout)
        onView(withText(R.string.help)).perform(click())

        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_main_help))

        val headers = mActivity.resources.getStringArray(R.array.help_headlines);

        onView(withText(R.string.help_main_headline_data_policy)).perform(click());
        ToolbarMatcher.matchToolbarTitle(headers[0]);
        pressBack();

        onView(withText(R.string.help_main_headline_tou)).perform(click());
        ToolbarMatcher.matchToolbarTitle(headers[1]);
        pressBack();

        onView(withText(R.string.help_main_headline_about_us)).perform(click());
        ToolbarMatcher.matchToolbarTitle(headers[2]);
        pressBack();

        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.headline_main_help))
    }
}
