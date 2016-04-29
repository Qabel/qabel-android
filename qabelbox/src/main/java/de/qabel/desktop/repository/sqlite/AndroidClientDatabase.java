package de.qabel.desktop.repository.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqldroid.SQLDroidConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import de.qabel.desktop.repository.sqlite.migration.AbstractMigration;
import de.qabel.desktop.repository.sqlite.migration.Migration1460367000CreateIdentitiy;
import de.qabel.desktop.repository.sqlite.migration.Migration1460367005CreateContact;
import de.qabel.desktop.repository.sqlite.migration.Migration1460367010CreateAccount;
import de.qabel.desktop.repository.sqlite.migration.Migration1460367020DropState;
import de.qabel.desktop.repository.sqlite.migration.Migration1460367035Entity;
import de.qabel.desktop.repository.sqlite.migration.Migration1460987825PreventDuplicateContacts;

public class AndroidClientDatabase extends AbstractClientDatabase implements ClientDatabase {

    private static final Logger logger = LoggerFactory.getLogger(DesktopClientDatabase.class);

    public AndroidClientDatabase(Connection connection) {
        super(connection);
    }

    @Override
    public long getVersion() throws SQLException {
        if (connection instanceof SQLDroidConnection) {
            android.database.sqlite.SQLiteDatabase db = ((SQLDroidConnection) connection).getDb()
                    .getSqliteDatabase();
            return db.getVersion();
        } else {
            return super.getVersion();
        }
    }

    @Override
    public synchronized void setVersion(long version) throws SQLException {
        if (connection instanceof SQLDroidConnection) {
            android.database.sqlite.SQLiteDatabase db = ((SQLDroidConnection) connection).getDb()
                    .getSqliteDatabase();
            db.setVersion((int) version);
        } else {
            super.setVersion(version);
        }
    }

    public AbstractMigration[] getMigrations(Connection connection) {
        return new AbstractMigration[]{
                new Migration1460367000CreateIdentitiy(connection),
                new Migration1460367005CreateContact(connection),
                new Migration1460367010CreateAccount(connection),
                new Migration1460367020DropState(connection),
                new Migration1460367035Entity(connection),
                new Migration1460987825PreventDuplicateContacts(connection)
        };
    }

    @Override
    public PreparedStatement prepare(String sql) throws SQLException {
        logger.trace(sql);
        if (connection instanceof SQLDroidConnection) {
            return new GeneratedKeysPreparedStatement(sql, (SQLDroidConnection) connection);
        } else {
            return super.prepare(sql);
        }
    }

}
