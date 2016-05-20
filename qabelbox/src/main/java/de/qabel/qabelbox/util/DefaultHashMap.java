package de.qabel.qabelbox.util;

import java.util.HashMap;

public class DefaultHashMap<k,v> extends HashMap<k, v> {

    private final DefaultValueFactory<k, v> valueFactory;

    public DefaultHashMap(DefaultValueFactory<k, v> valueFactory) {
        this.valueFactory = valueFactory;
    }

    @Override
    synchronized public v get(Object key) {
        k castedKey = (k) key;
        if (!containsKey(key)) {
            put(castedKey, valueFactory.defaultValueFor(castedKey));
        }
        return super.get(key);
    }

    public interface DefaultValueFactory<k, v> {

        v defaultValueFor(k key);

    }
}
