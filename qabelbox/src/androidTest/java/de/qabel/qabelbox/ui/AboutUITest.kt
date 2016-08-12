package de.qabel.qabelbox.ui

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.*
import de.qabel.qabelbox.R
import de.qabel.qabelbox.ui.matcher.ToolbarMatcher
import org.junit.Ignore
import org.junit.Test

class AboutUITest : AbstractUITest(){

    @Ignore("Drawer layout rebuild")
    @Test
    fun testShowAbout() {
        launchActivity(null)
        //DrawerActions.openDrawer(R.id.drawer_layout)
        onView(withText(R.string.action_about)).perform(click())

        ToolbarMatcher.matchToolbarTitle(mActivity.getString(R.string.action_about))
        onView(withId(R.id.about_licences_list)).check(matches(isDisplayed()))

        onView(withText(R.string.about_header_qabel_show_btn)).perform(click());
        onView(withText(R.string.qapl)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(R.string.ok)).perform(click())
        onView(withText(R.string.qapl)).check(doesNotExist())
    }
}
