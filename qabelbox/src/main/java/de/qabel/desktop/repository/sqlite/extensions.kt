package de.qabel.desktop.repository.sqlite

import de.qabel.core.config.Identities
import de.qabel.core.config.Identity


/** TODO
 * Kotlin currently not support AutoCloseable, just Closeable
 * Some hope : "TODO: Provide use kotlin package for AutoCloseable"
 * **/
inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            close()
        }
    }
}

/***
 * TODO set EntityMap public so we can use the baseclass to create a generic method,
 * that can map all keyIdentifier based Entities
 *
 */
fun Map<Int, List<String>>.mapEntities(key: Int, entities: Identities): List<Identity> {
    return getOrElse(key, { emptyList<String>() }).map { entities.getByKeyIdentifier(it) }
}

/**
 * fun <T : Entity> Map<Int, List<String>>.mapEntities(key : Int, entities : EntityMap<T>) : List<T> {
    return getOrElse(key, { emptyList<String>() }).map { entities.getByKeyIdentifier(it) }
}
 */
