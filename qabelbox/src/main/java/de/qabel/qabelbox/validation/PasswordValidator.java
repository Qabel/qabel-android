package de.qabel.qabelbox.validation;

import de.qabel.qabelbox.R.string;

public class PasswordValidator {
    public Integer validate(String accountName, String password, String passwordRepeat) {
        if (password.length() < ValidationConstraints.PASSWORD_MIN_LENGTH) {
            return string.password_to_short;
        }

        if (accountName != null && password.toLowerCase().contains(accountName.toLowerCase())) {
            return string.password_contains_user;
        }

        //Check for digits only
        char[] pwChars = password.toCharArray();
        boolean digitsOnly = true;
        for (char c : pwChars) {
            if (!Character.isDigit(c)) {
                digitsOnly = false;
                break;
            }
        }
        if (digitsOnly) {
            return string.password_digits_only;
        }

        //check if pw1 match pw2
        if (!password.equals(passwordRepeat)) {
            return string.create_account_passwords_dont_match;
        }
        return null;
    }
}
