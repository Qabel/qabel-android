package de.qabel.qabelbox.ui.idling;

import android.support.test.espresso.IdlingResource;

public class WaitResourceCallback implements IdlingResource.ResourceCallback {

    private boolean done = false;

    @Override
    public void onTransitionToIdle() {
        done = true;
    }

    public boolean isDone(){
        return done;
    }

    public void reset(){
        this.done = false;
    }


}
