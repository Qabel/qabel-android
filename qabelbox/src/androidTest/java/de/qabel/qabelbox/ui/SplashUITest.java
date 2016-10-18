package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.startup.activities.SplashActivity;

import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.hamcrest.Matchers.endsWith;


@Ignore
public class SplashUITest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule = new ActivityTestRule<>(
            SplashActivity.class, false, false);

    private AppPreference appPreference;

    @Before
    public void setUp() throws IOException {
        appPreference = new AppPreference(InstrumentationRegistry.getTargetContext());
        Intents.init();
    }

    @After
    public void releaseIntents() {
        Intents.release();
    }

    @Test
    public void testLaunchMainActivity() throws Throwable {
        appPreference.setWelcomeScreenShownAt(1);
        launch();
        Intents.intended(hasComponent(hasClassName(endsWith("MainActivity"))));
    }

    @Test
    public void testLaunchWelcomeScreen() throws Throwable {
        appPreference.setWelcomeScreenShownAt(0);
        launch();
        Intents.intended(hasComponent(hasClassName(endsWith("WelcomeScreenActivity"))));
    }

    protected void launch() {
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), SplashActivity.class);
        intent.putExtra(SplashActivity.SKIP_SPLASH, true);
        mActivityTestRule.launchActivity(intent);
    }

}
