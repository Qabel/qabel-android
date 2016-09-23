package de.qabel.qabelbox.index.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.index.view.views.IndexSearchFragment;

@ActivityScope
@Subcomponent(
        modules = IndexSearchModule.class
)
public interface IndexSearchComponent {
    void inject(IndexSearchFragment indexSearchFragment);
}
