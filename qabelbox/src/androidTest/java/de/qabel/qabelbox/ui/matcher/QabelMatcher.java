package de.qabel.qabelbox.ui.matcher;

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.hamcrest.Matchers.is;

public class QabelMatcher {

    public static Matcher<View> withListSize(final int size) {

        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(final View view) {

                return ((ListView) view).getChildCount() == size;
            }

            @Override
            public void describeTo(final Description description) {

                description.appendText("ListView should have " + size + " items");
            }
        };
    }

    public static ViewInteraction matchToolbarTitle(
            CharSequence title) {

        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(is(title))));
    }

    private static Matcher<Object> withToolbarTitle(
            final Matcher<CharSequence> textMatcher) {

        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            @Override
            public boolean matchesSafely(Toolbar toolbar) {

                return textMatcher.matches(toolbar.getTitle());
            }

            @Override
            public void describeTo(Description description) {

                description.appendText("match with toolbar title: ");
                textMatcher.describeTo(description);
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

}