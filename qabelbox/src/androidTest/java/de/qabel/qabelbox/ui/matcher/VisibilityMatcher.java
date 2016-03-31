package de.qabel.qabelbox.ui.matcher;

import android.view.View;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

class VisibilityMatcher extends BaseMatcher<View> {

    private final int visibility;

    public VisibilityMatcher(int visibility) {
        this.visibility = visibility;
    }

    @Override
    public void describeTo(Description description) {
        String visibilityName;
        if (visibility == View.GONE) visibilityName = "GONE";
        else if (visibility == View.VISIBLE) visibilityName = "VISIBLE";
        else visibilityName = "INVISIBLE";
        description.appendText("View visibility must has equals " + visibilityName);
    }

    @Override
    public boolean matches(Object o) {

        if (o == null) {
            if (visibility == View.GONE || visibility == View.INVISIBLE) return true;
            else if (visibility == View.VISIBLE) return false;
        }

        if (!(o instanceof View))
            throw new IllegalArgumentException("Object must be instance of View. Object is instance of " + o);
        return ((View) o).getVisibility() == visibility;
    }
}
