package de.qabel.qabelbox.navigation;

public interface Navigator {
    void selectCreateAccountActivity();

    void selectManageIdentitiesFragment();

    void selectContactsFragment(String activeContact);

    void selectContactsFragment();

    void selectHelpFragment();

    void selectAboutFragment();

    void selectFilesFragment();
}
