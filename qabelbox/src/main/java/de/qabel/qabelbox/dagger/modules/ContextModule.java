package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.config.AppPreference;

@Module
public class ContextModule {

    Context context;

    public ContextModule(Context context){
        this.context = context;
    }

    @Singleton @Provides Context providesContext(){
        return context;
    }

    @Singleton @Provides AppPreference providesAppPreferences(Context context){
        return new AppPreference(context);
    }

}
