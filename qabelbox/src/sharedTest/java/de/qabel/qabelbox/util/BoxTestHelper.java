package de.qabel.qabelbox.util;


import javax.inject.Inject;

import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.account.AccountManager;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.dagger.components.MockApplicationComponent;

public class BoxTestHelper {

    @Inject
    IdentityRepository identityRepository;
    @Inject
    ContactRepository contactRepository;
    @Inject
    AppPreference preference;

    @Inject
    AccountManager accountManager;

    public BoxTestHelper(QabelBoxApplication context) {
        ((MockApplicationComponent) context.getApplicationComponent()).inject(this);
    }


    public IdentityRepository getIdentityRepository(){
        return identityRepository;
    }

    public AppPreference getAppPreferences(){
        return preference;
    }

    public ContactRepository getContactRepository(){
        return contactRepository;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

}
