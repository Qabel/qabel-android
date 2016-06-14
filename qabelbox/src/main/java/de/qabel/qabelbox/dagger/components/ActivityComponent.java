package de.qabel.qabelbox.dagger.components;

import android.support.v7.app.AppCompatActivity;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = ActivityModule.class
)
public interface ActivityComponent {
    MainActivityComponent plus(MainActivityModule mainActivityModule);

    AppCompatActivity activity();
}


