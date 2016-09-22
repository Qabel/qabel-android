package de.qabel.core.repositories

import de.qabel.core.config.SyncSettingItem
import de.qabel.core.repository.EntityManager
import de.qabel.core.repository.HasId

class AndroidFakeEntityManager() : EntityManager() {

    override fun contains(entityType: Class<*>?, id: Int?): Boolean = false

    override fun <T : Any?> put(entityType: Class<T>?, entity: Any?, id: Int?) = Unit
    override fun <T : Any?> put(entityType: Class<T>?, entity: HasId?) = Unit
    override fun <T : Any?> put(entityType: Class<T>?, entity: SyncSettingItem?) = Unit

}
