package de.qabel.android.storage;

import de.qabel.core.crypto.QblECPublicKey;

public interface BoxExternal {
	QblECPublicKey getOwner();
	void setOwner(QblECPublicKey owner);
	boolean isAccessible();
}
