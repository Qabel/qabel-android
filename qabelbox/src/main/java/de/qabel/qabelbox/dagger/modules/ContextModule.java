package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.event.Event;
import de.qabel.core.event.EventDispatcher;
import de.qabel.core.event.EventSink;
import de.qabel.core.event.SubjectEventDispatcher;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.reporter.CrashReporter;
import de.qabel.qabelbox.reporter.CrashSubmitter;
import de.qabel.qabelbox.reporter.HockeyAppCrashReporter;
import de.qabel.qabelbox.reporter.HockeyAppCrashSubmitter;

@Module
public class ContextModule {

    Context context;

    public ContextModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    Context providesContext() {
        return context;
    }

    @Singleton
    @Provides
    AppPreference providesAppPreferences(Context context) {
        return new AppPreference(context);
    }

    @Provides
    @Singleton
    CrashReporter providesCrashReporter(Context context) {
        return new HockeyAppCrashReporter(context);
    }

    @Provides
    @Singleton
    CrashSubmitter providesCrashSubmitter(HockeyAppCrashSubmitter hockeyAppCrashSubmitter) {
        return hockeyAppCrashSubmitter;
    }

    @Provides
    @Singleton
    EventDispatcher providesEventDispatcher() {
        return new SubjectEventDispatcher();
    }

    @Provides
    EventSink providesEventSink(EventDispatcher dispatcher) {
        return dispatcher;
    }

}
