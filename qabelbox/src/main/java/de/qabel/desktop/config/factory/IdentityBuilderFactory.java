package de.qabel.desktop.config.factory;


public class IdentityBuilderFactory {
    private DropUrlGenerator dropUrlGenerator;

    public IdentityBuilderFactory(DropUrlGenerator dropUrlGenerator) {
        this.dropUrlGenerator = dropUrlGenerator;
    }

    public IdentityBuilder factory() {
        return new IdentityBuilder(dropUrlGenerator);
    }
}
