package de.qabel.qabelbox.ui.matcher;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.MatcherAssert.assertThat;

public class QabelMatcher {

    public static Matcher<View> withListSize(final int size) {

        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(final View view) {

                return ((ViewGroup) view).getChildCount() == size;
            }

            @Override
            public void describeTo(final Description description) {

                description.appendText("ListView should have " + size + " items");
            }
        };
    }

    public static Matcher<View> withProgress(final int expectedProgress) {
        return new BoundedMatcher<View, SeekBar>(SeekBar.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("expected: ");
                description.appendText("" + expectedProgress);
            }

            @Override
            public boolean matchesSafely(SeekBar seekBar) {
                return seekBar.getProgress() == expectedProgress;
            }
        };
    }


    public static Matcher<View> withDrawable(final int resourceId) {
        return new DrawableMatcher(resourceId);
    }


    public static ViewAssertion isVisible() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noView) {
                assertThat(view, new VisibilityMatcher(View.VISIBLE));
            }
        };
    }


    public static ViewAssertion isInvisible() {
        return new ViewAssertion() {
            public void check(View view, NoMatchingViewException noView) {
                assertThat(view, new VisibilityMatcher(View.INVISIBLE));
            }
        };
    }


}
