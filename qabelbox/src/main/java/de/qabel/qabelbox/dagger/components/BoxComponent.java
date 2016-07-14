package de.qabel.qabelbox.dagger.components;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.dagger.modules.ContextModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;
import de.qabel.qabelbox.dagger.modules.StorageModule;
import de.qabel.qabelbox.box.providers.BoxProvider;

@Component(modules = {ContextModule.class, RepositoryModule.class, StorageModule.class})
@Singleton
public interface BoxComponent {

    Context context();

    void inject(BoxProvider boxProvider);

}
