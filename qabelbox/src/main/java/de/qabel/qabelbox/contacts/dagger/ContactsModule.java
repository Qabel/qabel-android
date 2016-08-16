package de.qabel.qabelbox.contacts.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.contacts.view.presenters.ContactsPresenter;
import de.qabel.qabelbox.contacts.view.presenters.MainContactsPresenter;
import de.qabel.qabelbox.contacts.view.views.ContactsView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Module
public class ContactsModule extends ContactBaseModule {

    private ContactsView view;

    public ContactsModule(ContactsView view) {
        this.view = view;
    }

    @Provides
    public ContactsView provideContactsView() {
        return view;
    }

    @Provides
    public ContactsPresenter provideContactsPresenter(MainContactsPresenter presenter) {
        return presenter;
    }
}
