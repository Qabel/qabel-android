package de.qabel.qabelbox.dagger.components.modules;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.dagger.components.ActivityComponent;
import de.qabel.qabelbox.dagger.components.ApplicationComponent;
import de.qabel.qabelbox.dagger.components.DaggerApplicationComponent;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.ApplicationModule;
import de.qabel.qabelbox.dagger.modules.RepositoryModule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class DaggerSetupTest {

    @Test
    public void testApplication() throws Throwable {
        ApplicationComponent component = getApplicationComponent();
        assertThat(component.context(), equalTo((Context) RuntimeEnvironment.application));
    }

    public ApplicationComponent getApplicationComponent() {
        return DaggerApplicationComponent.builder()
                    .applicationModule(new ApplicationModule(
                            (QabelBoxApplication) RuntimeEnvironment.application))
                    .repositoryModule(new RepositoryModule())
                .build();
    }

    @Test
    public void testActivity() throws Throwable {
        AppCompatActivity activity = new AppCompatActivity();
        ActivityComponent activityComponent = getApplicationComponent()
                .plus(new ActivityModule(activity));
        assertThat(activityComponent.activity(), equalTo(activity));
    }

}
