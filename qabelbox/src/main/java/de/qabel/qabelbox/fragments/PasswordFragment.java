package de.qabel.qabelbox.fragments;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.constraints.ValidationConstraints;

public class PasswordFragment extends BaseIdentityFragment {

	public String validatePassword(String accountName, String password, String passwordRepeat) {

		if (password.length() < ValidationConstraints.PASSWORD_MIN_LENGTH) {
			return getString(R.string.password_to_short);
		}

		if (accountName != null && password.toLowerCase().contains(accountName.toLowerCase())) {
			return getString(R.string.password_contains_user);
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
			return getString(R.string.password_digits_only);
		}

		//check if pw1 match pw2
		if (!password.equals(passwordRepeat)) {
			return getString(R.string.create_account_passwords_dont_match);
		}
		return null;
	}
}
