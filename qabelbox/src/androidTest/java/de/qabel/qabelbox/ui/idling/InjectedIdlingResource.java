package de.qabel.qabelbox.ui.idling;

import android.support.test.espresso.IdlingResource;
import android.util.Log;

import de.qabel.qabelbox.listeners.IdleCallback;

public class InjectedIdlingResource implements IdleCallback, IdlingResource
{
    private static final String TAG = InjectedIdlingResource.class.getName();
    private boolean isIdle = true;
    private ResourceCallback callback;

    @Override
    public void busy() {
        Log.i(TAG, "Setting resource to busy");
        isIdle = false;
    }

    @Override
    public void idle() {
        if (!isIdle) {
            Log.i(TAG, "Setting resource to idle");
            if (callback != null) {
                callback.onTransitionToIdle();
            }
            isIdle = true;
        }
    }

    @Override
    public String getName() {
        return InjectedIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.callback = callback;
    }
}
