package de.qabel.desktop.repository.sqlite

import de.qabel.core.repository.sqlite.PragmaVersionAdapter
import de.qabel.core.repository.sqlite.VersionAdapter
import org.sqldroid.SQLDroidConnection
import java.sql.Connection
import kotlin.reflect.KProperty

class AndroidVersionAdapter(val connection: Connection): VersionAdapter {

    val default = PragmaVersionAdapter(connection)

    override operator fun getValue(thisRef: Any, property: KProperty<*>): Long {
        if (connection is SQLDroidConnection) {
            return connection.db.sqliteDatabase.version.toLong()
        } else {
            return default.getValue(thisRef, property)
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        if (connection is SQLDroidConnection) {
            connection.db.sqliteDatabase.version = value.toInt()
        } else {
            default.setValue(thisRef, property, value)
        }
    }

}
