package de.qabel.qabelbox.repository.persistence;

import java.util.Observable;

import de.qabel.core.config.Persistence;

public abstract class AbstractPersistenceRepository extends Observable {
    protected Persistence<String> persistence;

    public AbstractPersistenceRepository(Persistence<String> persistence) {
        this.persistence = persistence;
    }
}
