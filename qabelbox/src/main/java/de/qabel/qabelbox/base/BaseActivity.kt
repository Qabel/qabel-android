package de.qabel.qabelbox.base

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import de.qabel.qabelbox.QabelBoxApplication
import de.qabel.qabelbox.dagger.components.ApplicationComponent
import de.qabel.qabelbox.listeners.IntentListener
import de.qabel.qabelbox.listeners.toIntentFilter

abstract class BaseActivity : AppCompatActivity() {

    protected open val intentListeners = emptyList<IntentListener>()

    val applicationComponent: ApplicationComponent
        get() = QabelBoxApplication.getApplicationComponent(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentListeners.forEach {
            registerReceiver(it.receiver, it.toIntentFilter())
        }
    }

    override fun onDestroy() {
        intentListeners.forEach {
            unregisterReceiver(it.receiver)
        }
        super.onDestroy()
    }

}
