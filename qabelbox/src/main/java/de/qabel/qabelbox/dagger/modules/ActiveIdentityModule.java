package de.qabel.qabelbox.dagger.modules;

import android.util.Log;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.exception.EntityNotFoundException;
import de.qabel.core.repository.exception.PersistenceException;
import de.qabel.qabelbox.base.ActiveIdentityActivity;
import de.qabel.qabelbox.base.MainActivity;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.navigation.Navigator;

@ActivityScope
@Module
public class ActiveIdentityModule {

    private final ActiveIdentityActivity activeIdentityActivity;

    public ActiveIdentityModule(ActiveIdentityActivity activeIdentityActivity) {
        this.activeIdentityActivity = activeIdentityActivity;
    }

    @Provides
    public ActiveIdentityActivity provideMainActivity() {
        return activeIdentityActivity;
    }

    @Provides
    Identity provideActiveIdentity(IdentityRepository identityRepository,
                                   AppPreference sharedPreferences) {
        String identityKeyId = activeIdentityActivity.getActiveIdentityKey();
        String lastActiveId = sharedPreferences.getLastActiveIdentityKey();
        Identity activeIdentity = null;
        if (identityKeyId != null) {
            try {
                activeIdentity = identityRepository.find(identityKeyId);

                //TODO Working very bad. Redesign identity selection!
                //Show Toast if active identity is not last active identity.
                /*if (activeIdentity != null && lastActiveId != null && lastActiveId.equals(identityKeyId)) {
                    Toast.makeText(activeIdentityActivity, activeIdentityActivity.getString(R.string.active_identity_changed,
                            activeIdentity.getAlias()), Toast.LENGTH_SHORT).show();
                }*/
            } catch (EntityNotFoundException | PersistenceException entityNotFoundException) {
                Log.w("ActiveIdentityModule", "Given Identity not found (" + identityKeyId + ")");
            }
        }
        if (activeIdentity == null) {
            try {
                if (lastActiveId != null) {
                    activeIdentity = identityRepository.find(lastActiveId);
                }
            } catch (EntityNotFoundException | PersistenceException entityNotFoundException) {
                Log.w("ActiveIdentityModule", "Last-active identity not found");
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
