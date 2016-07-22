package de.qabel.qabelbox.dagger.components;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.dagger.modules.BoxModule;
import de.qabel.qabelbox.box.provider.BoxProvider;
import de.qabel.qabelbox.dagger.modules.ContextModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;

@Component(modules = {BoxModule.class, RepositoryModule.class, ContextModule.class})
@Singleton
public interface BoxComponent {

    void inject(BoxProvider boxProvider);

}
