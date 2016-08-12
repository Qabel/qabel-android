package de.qabel.qabelbox.contacts.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase;
import de.qabel.qabelbox.contacts.view.presenters.ContactDetailsPresenter;
import de.qabel.qabelbox.contacts.view.presenters.ContactEditPresenter;
import de.qabel.qabelbox.contacts.view.presenters.MainContactDetailsPresenter;
import de.qabel.qabelbox.contacts.view.presenters.MainContactEditPresenter;
import de.qabel.qabelbox.contacts.view.views.ContactDetailsView;
import de.qabel.qabelbox.contacts.view.views.ContactEditView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.navigation.Navigator;

@ActivityScope
@Module
public class ContactEditModule extends ContactBaseModule {

    private ContactEditView view;

    public ContactEditModule(ContactEditView view) {
        this.view = view;
    }

    @Provides
    public ContactEditView provideContactsView() {
        return view;
    }

    @Provides
    public ContactEditPresenter provideContactEditPresenter(MainContactEditPresenter presenter){
        return presenter;
    }

}
