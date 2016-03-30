package de.qabel;

public class IIdentity {
    private String alias;
    private String id;

    public IIdentity(String alias, String id) {
        this.alias = alias;
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public String getId() {
        return id;
    }
}
