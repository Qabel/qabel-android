package de.qabel.qabelbox.identity.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.identity.view.IdentityDetailsView;
import de.qabel.qabelbox.identity.view.presenter.IdentityDetailsPresenter;
import de.qabel.qabelbox.identity.view.presenter.MainIdentityDetailsPresenter;

@ActivityScope
@Module
public class IdentityDetailsModule extends IdentityBaseModule {

    private IdentityDetailsView detailsView;

    public IdentityDetailsModule(IdentityDetailsView view) {
        this.detailsView = view;
    }

    @Provides
    public IdentityDetailsView providesIdentityDetailsView() {
        return detailsView;
    }

    @Provides
    public IdentityDetailsPresenter providesIdentityDetailsPresenter(MainIdentityDetailsPresenter presenter) {
        return presenter;
    }

}
