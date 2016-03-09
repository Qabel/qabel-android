package de.qabel.android.util;

import android.content.Context;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.android.QabelBoxApplication;
import de.qabel.android.helper.PrefixGetter;

/**
 * Created by danny on 02.03.16.
 */
public class IdentityHelper {
	/**
	 * create identity onthefile
	 *
	 * @param context
	 * @param identName
	 * @param prefix  if prefix null, get a real prefix
	 * @return
	 */
	public static Identity createIdentity(Context context, String identName, String prefix) {
		URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
		DropServer dropServer = new DropServer(uri, "", true);
		DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
		DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
		Collection<DropURL> dropURLs = new ArrayList<>();
		dropURLs.add(dropURL);
		if (prefix == null) {
			prefix = new PrefixGetter().getPrefix(context);
		}
		Identity identity = new Identity(identName,
				dropURLs, new QblECKeyPair());
		identity.getPrefixes().add(prefix);
		return identity;
	}
}
