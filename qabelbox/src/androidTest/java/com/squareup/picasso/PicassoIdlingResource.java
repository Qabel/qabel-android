package com.squareup.picasso;

import android.app.Activity;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.lifecycle.ActivityLifecycleCallback;
import android.support.test.runner.lifecycle.Stage;

import java.lang.ref.WeakReference;

public class PicassoIdlingResource implements IdlingResource, ActivityLifecycleCallback {
    protected ResourceCallback callback;

    WeakReference<Picasso> picassoWeakReference;

    @Override
    public String getName() {
        return "PicassoIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        if (isIdle()) {
            notifyDone();
            return true;
        } else {
            return false;
        }
    }

    public boolean isIdle() {
        return picassoWeakReference == null
                || picassoWeakReference.get() == null
                || picassoWeakReference.get().targetToAction.isEmpty();
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        callback = resourceCallback;
    }

    void notifyDone() {
        if (callback != null) {
            callback.onTransitionToIdle();
        }
    }

    @Override
    public void onActivityLifecycleChanged(Activity activity, Stage stage) {
        switch (stage) {
            case CREATED:
                init(activity);
                break;
            case STOPPED:
                picassoWeakReference = null;
                break;
            default:
        }
    }

    public void init(Activity activity) {
        picassoWeakReference = new WeakReference<>(Picasso.with(activity));
    }
}

