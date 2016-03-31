package de.qabel.qabelbox.validation;


import de.qabel.qabelbox.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PasswordValidatorTest {

    private PasswordValidator passwordValidator = new PasswordValidator();

    private static final String ACCOUNT_NAME = "test";

    private static final String PASSWORD_ACCOUNTNAME = "tESt12345";
    private static final String PASSWORD_NUMERIC = "12345678";
    private static final String PASSWORD_SHORT = "blub123";

    private static final String PASSWORD_VALID = "aBc9XyU567";

    @Test
    public void testPasswordsNotMatch() {
        Integer result = passwordValidator.validate(ACCOUNT_NAME, PASSWORD_VALID, PASSWORD_SHORT);
        assertNotNull(result);
        assertEquals(R.string.create_account_passwords_dont_match, result.intValue());
    }

    @Test
    public void testPasswordToShort() {
        Integer result = passwordValidator.validate(ACCOUNT_NAME, PASSWORD_SHORT, PASSWORD_SHORT);
        assertNotNull(result);
        assertEquals(R.string.password_to_short, result.intValue());
    }

    @Test
    public void testPasswordWithAccount() {
        Integer result = passwordValidator.validate(ACCOUNT_NAME, PASSWORD_ACCOUNTNAME, PASSWORD_ACCOUNTNAME);
        assertNotNull(result);
        assertEquals(R.string.password_contains_user, result.intValue());
    }

    @Test
    public void testPasswordIsNumericOnly() {
        Integer result = passwordValidator.validate(ACCOUNT_NAME, PASSWORD_NUMERIC, PASSWORD_NUMERIC);
        assertNotNull(result);
        assertEquals(R.string.password_digits_only, result.intValue());
    }


}
