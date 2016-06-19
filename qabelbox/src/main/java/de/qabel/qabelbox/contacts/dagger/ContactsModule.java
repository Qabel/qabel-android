package de.qabel.qabelbox.contacts.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase;
import de.qabel.qabelbox.contacts.interactor.MainContactsUseCase;
import de.qabel.qabelbox.contacts.navigation.ContactsNavigator;
import de.qabel.qabelbox.contacts.navigation.MainContactsNavigator;
import de.qabel.qabelbox.contacts.view.ContactsView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.contacts.view.presenters.ContactsPresenter;
import de.qabel.qabelbox.contacts.view.presenters.MainContactsPresenter;

@ActivityScope
@Module
public class ContactsModule {

    private ContactsView view;

    public ContactsModule(ContactsView view) {
        this.view = view;
    }

    @Provides
    public ContactsView provideContactsView() {
        return view;
    }

    @Provides
    public ContactsUseCase provideContactsUseCase(Identity identity,
                                                  IdentityRepository identityRepository,
                                                  ContactRepository contactRepository) {
        return new MainContactsUseCase(identity, identityRepository, contactRepository);
    }

    @Provides
    public ContactsNavigator providesContactsNavigator(Identity activeIdentity){
        return new MainContactsNavigator(activeIdentity);
    }

    @Provides
    public ContactsPresenter provideContactsPresenter(ContactsUseCase contactsUseCase) {
        return new MainContactsPresenter(view, contactsUseCase);
    }
}
