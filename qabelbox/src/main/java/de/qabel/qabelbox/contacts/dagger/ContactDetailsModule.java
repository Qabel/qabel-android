package de.qabel.qabelbox.contacts.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase;
import de.qabel.qabelbox.contacts.view.presenters.ContactDetailsPresenter;
import de.qabel.qabelbox.contacts.view.presenters.MainContactDetailsPresenter;
import de.qabel.qabelbox.contacts.view.views.ContactDetailsView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Module
public class ContactDetailsModule extends ContactBaseModule {

    private ContactDetailsView view;

    public ContactDetailsModule(ContactDetailsView view) {
        this.view = view;
    }

    @Provides
    public ContactDetailsView provideContactsView() {
        return view;
    }

    @Provides
    public ContactDetailsPresenter provideContactDetailsPresenter(ContactsUseCase useCase){
        return new MainContactDetailsPresenter(view, useCase);
    }

}
