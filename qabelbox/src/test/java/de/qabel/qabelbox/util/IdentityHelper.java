package de.qabel.qabelbox.util;

import android.content.Context;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.config.factory.IdentityBuilder;
import de.qabel.qabelbox.QabelBoxApplication;

/**
 * Created by danny on 02.03.16.
 */
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
            identity.getPrefixes().add(prefix);
            return identity;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
