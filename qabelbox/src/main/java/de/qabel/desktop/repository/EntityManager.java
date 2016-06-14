package de.qabel.desktop.repository;

import java.util.WeakHashMap;

import de.qabel.core.config.SyncSettingItem;

public class EntityManager {
    private WeakHashMap<Class, WeakHashMap<Integer, Object>> entities = new WeakHashMap<>();

    public boolean contains(Class entityType, int id) {
        if (!entities.containsKey(entityType)) {
            return false;
        }
        return entities.get(entityType).containsKey(id);
    }

    public synchronized <T> void put(Class<T> entityType, SyncSettingItem entity) {
        put(entityType, entity, entity.getId());
    }

    public synchronized <T> void put(Class<T> entityType, HasId entity) {
        put(entityType, entity, entity.getId());
    }

    public <T> void put(Class<T> entityType, Object entity, int id) {
        if (!entities.containsKey(entityType)) {
            entities.put(entityType, new WeakHashMap<Integer, Object>());
        }
        entities.get(entityType).put(id, entity);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> entityType, int id) {
        return (T)entities.get(entityType).get(id);
    }

    public void clear() {
        entities.clear();
    }
}
