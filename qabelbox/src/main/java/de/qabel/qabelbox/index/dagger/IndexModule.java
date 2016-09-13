package de.qabel.qabelbox.index.dagger;

import android.content.Context;

import org.apache.http.impl.client.HttpClients;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.index.IndexService;
import de.qabel.core.index.MainIndexService;
import de.qabel.core.index.server.ExternalContactsAccessor;
import de.qabel.core.index.server.IndexHTTP;
import de.qabel.core.index.server.IndexHTTPLocation;
import de.qabel.core.index.server.IndexServer;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.index.AndroidContactsAccessor;
import de.qabel.qabelbox.index.preferences.AndroidIndexPreferences;
import de.qabel.qabelbox.index.preferences.IndexPreferences;

@Module
public class IndexModule {

    @Provides
    IndexServer providesIndexServer(Context context) {
        //TODO JAVAAAAA
        return new IndexHTTP(new IndexHTTPLocation(context.getString(R.string.indexServer)), HttpClients.createMinimal());
    }

    @Provides
    IndexService providesIndexInteractor(IndexServer indexServer,
                                         ContactRepository contactRepository,
                                         IdentityRepository identityRepository) {
        return new MainIndexService(indexServer, contactRepository, identityRepository);
    }

    @Provides
    IndexPreferences providesIndexPreferences(Context context){
        return new AndroidIndexPreferences(context);
    }

    @Provides
    ExternalContactsAccessor providesContactsAccessor(Context context) {
        return new AndroidContactsAccessor(context);
    }
}
