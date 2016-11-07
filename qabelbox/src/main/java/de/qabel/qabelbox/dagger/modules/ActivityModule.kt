package de.qabel.qabelbox.dagger.modules

import android.support.v7.app.AppCompatActivity

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.base.ActivityStartup
import de.qabel.qabelbox.base.SanityCheckStartup
import de.qabel.qabelbox.communication.connection.ConnectivityManager

@Module
class ActivityModule(private val activity: AppCompatActivity) {

    @Provides internal fun provideActivity(): AppCompatActivity {
        return this.activity
    }

    @Provides internal fun provideConnectivityManager(): ConnectivityManager {
        return ConnectivityManager(activity)

    }

    @Provides
    internal fun provideActivityStartup(startup: SanityCheckStartup): ActivityStartup {
        return startup
    }

}
