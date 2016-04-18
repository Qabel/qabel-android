package de.qabel.qabelbox.providers;

import android.content.ContentResolver;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Set;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.services.RoboLocalQabelService;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class InternalContentProviderTest {

    private static final String AUTHORITY = "content://de.qabel.debug.drop";

    private Context context;
    private MockedInternalContentProvider contentProvider;
    private ContentResolver contentResolver;
    private ShadowContentResolver shadowContentResolver;
    private RoboLocalQabelService service;
    private Identity identity;
    private Identity identity2;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        contentProvider = new MockedInternalContentProvider(context);
        service = new RoboLocalQabelService();
        service.onCreate();
        contentProvider.injectService(service);
        contentProvider.onCreate();
        contentResolver = context.getContentResolver();
        shadowContentResolver = Shadows.shadowOf(contentResolver);
        ShadowContentResolver.registerProvider(AUTHORITY, contentProvider);
        identity = IdentityHelper.createIdentity("foo", null);
        identity2 = IdentityHelper.createIdentity("bar", null);
        service.addIdentity(identity);
        service.addIdentity(identity2);
    }

    @Test
    public void testOnCreate() throws Exception {
        assertNotNull(contentProvider.context);
        assertNotNull(contentProvider.mService);
    }

    @Test
    public void testIdentitiesFound() throws Exception {
        Set<Identity> identitySet = contentProvider.getIdentities();
        assertTrue(identitySet.contains(identity));
        assertTrue(identitySet.contains(identity2));
        assertTrue(contentProvider.getDataBases().containsKey(identity));
        assertTrue(contentProvider.getDataBases().containsKey(identity2));
    }

    @Test
    public void testInsert() throws Exception {

    }

    @Test
    public void testQuery() throws Exception {

    }

    @Test
    public void testGetType() throws Exception {

    }

    @Test
    public void testDelete() throws Exception {

    }

    @Test
    public void testUpdate() throws Exception {

    }
}
