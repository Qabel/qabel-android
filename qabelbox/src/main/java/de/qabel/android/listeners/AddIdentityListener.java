package de.qabel.android.listeners;

import de.qabel.core.config.Identity;

public interface AddIdentityListener {

    void addIdentity(Identity identity);

    void cancelAddIdentity();
}