package de.qabel.qabelbox.util;


import javax.inject.Inject;

import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.dagger.components.MockApplicationComponent;
import de.qabel.qabelbox.storage.BoxManager;

public class BoxTestHelper {

    @Inject
    BoxManager boxManager;
    @Inject
    IdentityRepository identityRepository;
    @Inject
    ContactRepository contactRepository;
    @Inject
    AppPreference preference;

    public BoxTestHelper(QabelBoxApplication context){
        ((MockApplicationComponent)context.getApplicationComponent()).inject(this);
    }

    public BoxManager createBoxManager() {
        return boxManager;
    }

    public IdentityRepository createIdentityRepository(){
        return identityRepository;
    }

    public AppPreference createAppPreferences(){
        return preference;
    }

    public ContactRepository createContactRepository(){
        return contactRepository;
    }

}
