package de.qabel.qabelbox.dagger.modules;

import android.util.Log;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundException;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.navigation.Navigator;

import static de.qabel.qabelbox.activities.MainActivity.ACTIVE_IDENTITY;

@ActivityScope
@Module
public class MainActivityModule {

    private final MainActivity mainActivity;

    public MainActivityModule(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Provides
    public MainActivity provideMainActivity() {
        return mainActivity;
    }

    @Provides
    Identity provideActiveIdentity(IdentityRepository identityRepository,
                                   AppPreference sharedPreferences) {
        String identityKeyId = mainActivity.getIntent().getStringExtra(ACTIVE_IDENTITY);
        Identity activeIdentity = null;
        if (identityKeyId != null) {
            try {
                activeIdentity = identityRepository.find(identityKeyId);
            } catch (EntityNotFoundException | PersistenceException entityNotFoundException) {
                Log.w("MainActivityModule", "Given Identity not found (" + identityKeyId + ")");
            }
        }
        if (activeIdentity == null) {
            String keyId = sharedPreferences.getLastActiveIdentityKey();
            try {
                if (keyId != null) {
                    activeIdentity = identityRepository.find(keyId);
                }
            } catch (EntityNotFoundException | PersistenceException entityNotFoundException) {
                Log.w("MainActivityModule", "Last-active identity not found");
            }

            if (activeIdentity == null) {
                try {
                    Identities identities = identityRepository.findAll();
                    if (identities.getIdentities().size() == 0) {
                        throw new IllegalStateException("No Identity available");
                    }
                    return identities.getIdentities().iterator().next();
                } catch (PersistenceException e) {
                    throw new IllegalStateException("Starting MainActivity without Identity");
                }
            }
        }
        return activeIdentity;
    }

    @ActivityScope
    @Provides
    Navigator provideNavigator(MainNavigator navigator) {
        return navigator;
    }


}
