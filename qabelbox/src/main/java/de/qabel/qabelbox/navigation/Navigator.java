package de.qabel.qabelbox.navigation;

public interface Navigator {
    void selectCreateAccountActivity();

    void selectManageIdentitiesFragment();

    void selectChatFragment(String activeContact);

    void selectChatFragment();

    void selectHelpFragment();

    void selectAboutFragment();

    void selectFilesFragment();
}
