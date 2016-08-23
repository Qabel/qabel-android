package de.qabel.core.repositories

import de.qabel.core.repository.sqlite.AbstractClientDatabase
import de.qabel.core.repository.sqlite.ClientDatabase
import de.qabel.core.repository.sqlite.migration.*
import de.qabel.core.util.QblLogger
import de.qabel.core.util.trace
import org.sqldroid.SQLDroidConnection
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class AndroidClientDatabase(connection: Connection) : AbstractClientDatabase(connection), ClientDatabase, QblLogger {

    private val versionAdapter = AndroidVersionAdapter(connection)

    override var version by versionAdapter

    override fun getMigrations(connection: Connection): Array<AbstractMigration> {
        return arrayOf(Migration1460367000CreateIdentitiy(connection),
                Migration1460367005CreateContact(connection),
                Migration1460367010CreateAccount(connection),
                Migration1460367020DropState(connection),
                Migration1460367035Entity(connection),
                Migration1460987825PreventDuplicateContacts(connection),
                Migration1460997040ChatDropMessage(connection),
                Migration1460997041RenameDropState(connection),
                Migration1460997042ExtendContact(connection))
    }

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
