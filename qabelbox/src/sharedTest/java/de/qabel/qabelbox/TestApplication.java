package de.qabel.qabelbox;

import de.qabel.qabelbox.dagger.components.DaggerMockApplicationComponent;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;

public class TestApplication extends QabelBoxApplication {

    @Override
    public de.qabel.qabelbox.dagger.components.ApplicationComponent initialiseInjector() {
        return DaggerMockApplicationComponent.builder()
        .applicationModule(new ApplicationModule(this)).build();
    }
}
