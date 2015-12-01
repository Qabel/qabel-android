package de.qabel;


import java.io.Serializable;

public class IContact implements Serializable {

    private String alias;
    private String ecPublicKey;
    private String contactOwnerId;
    private String contactOwnerAlias;

    public IContact(String alias, String contactOwnerId, String ecPublicKey, String contactOwnerAlias) {
        this.alias = alias;
        this.contactOwnerId = contactOwnerId;
        this.ecPublicKey = ecPublicKey;
        this.contactOwnerAlias = contactOwnerAlias;
    }

    public String getAlias() {
        return alias;
    }

    public String getContactOwnerId() {
        return contactOwnerId;
    }

    public String getEcPublicKey() {
        return ecPublicKey;
    }

    public String getContactOwnerAlias() {
        return contactOwnerAlias;
    }
}
