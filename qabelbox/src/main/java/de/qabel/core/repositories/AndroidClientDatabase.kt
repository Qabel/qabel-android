package de.qabel.core.repositories

import de.qabel.chat.repository.sqlite.ChatClientDatabase
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.sqlite.ClientDatabase
import org.sqldroid.SQLDroidConnection
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class AndroidClientDatabase(connection: Connection) : ChatClientDatabase(connection), ClientDatabase, QabelLog {

    private val versionAdapter = AndroidVersionAdapter(connection)

    override var version by versionAdapter

    @Throws(SQLException::class)
    override fun prepare(sql: String): PreparedStatement {
        if (connection is SQLDroidConnection) {
            trace(sql)
            return GeneratedKeysPreparedStatement(sql, connection)
        } else {
            return super.prepare(sql)
        }
    }
}
