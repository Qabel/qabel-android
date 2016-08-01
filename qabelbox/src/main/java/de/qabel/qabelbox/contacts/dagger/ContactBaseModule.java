package de.qabel.qabelbox.contacts.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identity;
import de.qabel.core.repository.ContactRepository;
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase;
import de.qabel.qabelbox.contacts.interactor.MainContactsUseCase;

@Module
public abstract class ContactBaseModule {

    @Provides
    public ContactsUseCase provideContactsUseCase(Identity identity,
                                                  ContactRepository contactRepository) {
        return new MainContactsUseCase(identity, contactRepository);
    }

}
