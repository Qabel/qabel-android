package de.qabel.qabelbox.ui

import de.qabel.qabelbox.R
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import de.qabel.qabelbox.ui.action.QabelViewAction

interface UITest {

}

fun UITest.onViewVisibleText(textRes: Int): ViewInteraction =
        onView(withText(textRes)).check(matches(isDisplayed()))

fun UITest.onViewVisibleText(text: String): ViewInteraction =
        onView(withText(text)).check(matches(isDisplayed()))

fun UITest.enterText(viewId: Int, text: String) =
        onView(withId(viewId)).perform(QabelViewAction.setText(text))

fun UITest.performClickText(resId: Int) {
    onView(withText(resId)).perform(click())
}
