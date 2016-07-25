package de.qabel.qabelbox.dagger.modules;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.box.dto.VolumeRoot;
import de.qabel.qabelbox.box.interactor.BoxProviderUseCase;
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase;
import de.qabel.qabelbox.box.interactor.MockFileBrowserUseCase;
import de.qabel.qabelbox.box.interactor.ProviderUseCase;
import de.qabel.qabelbox.box.interactor.VolumeManager;

@Module
public class BoxModule {

    @Singleton
    @Provides
    ProviderUseCase provideProviderUseCase(BoxProviderUseCase useCase) {
        return useCase;
    }

    @Singleton
    @Provides
    VolumeManager provideVolumeManager() {
        final FileBrowserUseCase useCase = new MockFileBrowserUseCase();
        return new VolumeManager() {
            @NotNull
            @Override
            public List<VolumeRoot> getRoots() {
                List<VolumeRoot> lst = new ArrayList<>();
                lst.add(new VolumeRoot("Root ID", "docid", "alias"));
                return lst;
            }

            @NotNull
            @Override
            public FileBrowserUseCase fileBrowser(@NotNull String rootID) {
                return useCase;
            }
        };
    }

}
