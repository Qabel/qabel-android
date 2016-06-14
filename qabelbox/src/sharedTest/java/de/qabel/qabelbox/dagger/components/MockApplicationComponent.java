package de.qabel.qabelbox.dagger.components;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.dagger.modules.AccountModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.MockStorageModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;
import de.qabel.qabelbox.util.BoxTestHelper;

@Component(modules = {ApplicationModule.class, RepositoryModule.class, AccountModule.class, MockStorageModule.class})
@Singleton
public interface MockApplicationComponent extends ApplicationComponent {

    void inject(BoxTestHelper testHelper);
}
