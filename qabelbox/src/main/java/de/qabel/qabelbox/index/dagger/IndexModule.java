package de.qabel.qabelbox.index.dagger;

import android.content.Context;

import org.apache.http.impl.client.HttpClients;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.index.IndexInteractor;
import de.qabel.core.index.MainIndexInteractor;
import de.qabel.core.index.server.ExternalContactsAccessor;
import de.qabel.core.index.server.IndexHTTP;
import de.qabel.core.index.server.IndexHTTPLocation;
import de.qabel.core.index.server.IndexServer;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.index.AndroidContactsAccessor;

@Module
public class IndexModule {

    @Provides
    IndexServer providesIndexServer(Context context) {
        //TODO JAVAAAAA
        return new IndexHTTP(new IndexHTTPLocation(context.getString(R.string.indexServer)), HttpClients.createMinimal());
    }

    @Provides
    IndexInteractor providesIndexInteractor(IndexServer indexServer,
                                            ContactRepository contactRepository,
                                            IdentityRepository identityRepository) {
        return new MainIndexInteractor(indexServer, contactRepository, identityRepository);
    }

    @Provides
    ExternalContactsAccessor providesContactsAccessor(Context context) {
        return new AndroidContactsAccessor(context);
    }
}
