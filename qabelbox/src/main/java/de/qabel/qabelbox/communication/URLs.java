package de.qabel.qabelbox.communication;

/**
 * Created by danny on 26.01.2016.
 */
public class URLs {

    //define base urls
    final static String BASE_ACCOUNTING ="https://test-accounting.qabel.de";

    //accounting
    final static String REGISTER = BASE_ACCOUNTING +"/api/v0/auth/registration/";
    final static String LOGIN = BASE_ACCOUNTING +"/api/v0/auth/login/";
    final static String LOGOUT = BASE_ACCOUNTING +"/api/v0/auth/logout/";
    final static String PASSWORD_CHANGE = BASE_ACCOUNTING +"/api/v0/auth/password/change/";
    final static String PASSWORD_RESET = BASE_ACCOUNTING +"/api/v0/auth/password/reset/";

    //prefix
    final static String PREFIX_SERVER = BASE_ACCOUNTING +"/api/v0/prefix/";

}
