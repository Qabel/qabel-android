package de.qabel.qabelbox.util;

import java.net.URISyntaxException;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.config.factory.IdentityBuilder;
import de.qabel.qabelbox.QabelBoxApplication;

public class IdentityHelper {

    private static Identity identity;

    /**
     * create identity onthefile
     *
     * @param identName
     * @param prefix
     * @return
     */
    public static Identity createIdentity(String identName, String prefix) {
        try {
            identity = new IdentityBuilder(new DropUrlGenerator(QabelBoxApplication.DEFAULT_DROP_SERVER))
                    .withAlias(identName).build();
            if (prefix == null) {
                prefix = "test";
            }
            identity.getPrefixes().add(prefix);
            return identity;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Contact createContact(String contactName) {
        try {
            identity = new IdentityBuilder(new DropUrlGenerator(QabelBoxApplication.DEFAULT_DROP_SERVER))
                    .withAlias(contactName).build();

            return new Contact(contactName, identity.getDropUrls(), identity.getEcPublicKey());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
