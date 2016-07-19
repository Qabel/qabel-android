package de.qabel.qabelbox.dagger.components;

import javax.inject.Singleton;

import dagger.Component;
import de.qabel.qabelbox.dagger.modules.BoxModule;
import de.qabel.qabelbox.box.provider.BoxProvider;

@Component(modules = {BoxModule.class})
@Singleton
public interface BoxComponent {

    void inject(BoxProvider boxProvider);

}
