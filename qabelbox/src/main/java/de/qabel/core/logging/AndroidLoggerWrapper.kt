package de.qabel.core.logging

import android.util.Log
import de.qabel.core.logging.QabelLoggerManager.QabelLoggerFactory

class AndroidLoggerWrapper(clazz: Class<*>) : QabelLoggerWrapper {

    object Factory : QabelLoggerFactory {
        override fun <T> createLogger(clazz: Class<T>): QabelLoggerWrapper =
                AndroidLoggerWrapper(clazz)
    }

    private val TAG = clazz.simpleName

    override fun trace(msg: Any, vararg args: Any) {
        Log.v(TAG, msg.toString().format(args))
    }

    override fun info(msg: Any, vararg args: Any) {
        Log.i(TAG, msg.toString().format(args))
    }

    override fun debug(msg: Any, vararg args: Any) {
        Log.d(TAG, msg.toString().format(args))
    }

    override fun warn(msg: Any, vararg args: Any) {
        Log.v(TAG, msg.toString().format(args))
    }

    override fun error(msg: Any?, exception: Throwable?) {
        Log.e(TAG, msg?.toString(), exception)
    }

}


