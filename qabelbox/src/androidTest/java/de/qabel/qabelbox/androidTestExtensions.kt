package de.qabel.qabelbox

import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.assertion.ViewAssertions.matches
import org.mockito.Mockito


fun <T> stubResult(methodCall: T, result: T)
        = Mockito.`when`(methodCall).thenReturn(result)

fun ViewInteraction.hasText(text: String) {
    check(matches(withText(text)))
}

