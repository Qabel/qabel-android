package de.qabel.qabelbox.storage.model;

import de.qabel.core.crypto.QblECPublicKey;

public interface BoxExternal {
    QblECPublicKey getOwner();

    void setOwner(QblECPublicKey owner);

    boolean isAccessible();
}
