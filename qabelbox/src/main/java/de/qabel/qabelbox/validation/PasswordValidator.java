package de.qabel.qabelbox.validation;

import android.app.Fragment;

import de.qabel.qabelbox.R;

public class PasswordValidator {

	private Fragment fragment;

	public PasswordValidator(Fragment fragment) {
		this.fragment = fragment;
	}

	public String validate(String accountName, String password, String passwordRepeat) {

		if (password.length() < ValidationConstraints.PASSWORD_MIN_LENGTH) {
			return fragment.getString(R.string.password_to_short);
		}

		if (accountName != null && password.toLowerCase().contains(accountName.toLowerCase())) {
			return fragment.getString(R.string.password_contains_user);
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
			return fragment.getString(R.string.password_digits_only);
		}

		//check if pw1 match pw2
		if (!password.equals(passwordRepeat)) {
			return fragment.getString(R.string.create_account_passwords_dont_match);
		}
		return null;
	}
}
