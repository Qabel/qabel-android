package de.qabel.qabelbox;

import android.app.Application;

import de.qabel.qabelbox.providers.BoxProvider;

public class QabelBoxApplication extends Application {
    public static BoxProvider boxProvider;

    public BoxProvider getProvider() {
        return boxProvider;
    }
}
