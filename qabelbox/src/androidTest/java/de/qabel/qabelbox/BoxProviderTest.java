package de.qabel.qabelbox;

import android.test.ProviderTestCase2;

import de.qabel.qabelbox.providers.BoxProvider;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoxProviderTest extends ProviderTestCase2<BoxProvider>{

    /**
     * Constructor.
     *
     * @param providerClass     The class name of the provider under test
     * @param providerAuthority The provider's authority string
     */
    public BoxProviderTest(Class<BoxProvider> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    public BoxProviderTest() {
        this(BoxProvider.class, "de.qabel.qabelbox.providers.BoxProvider");
    }

    public void testInit() {
        assertNotNull(getMockContext());
        assertNotNull(getMockContentResolver());
        assertThat(getProvider().getClass().getName(),
                is("de.qabel.qabelbox.providers.BoxProvider"));
    }
}
