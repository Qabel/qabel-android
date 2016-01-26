package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.BaseWizwardActivity;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 19.01.16.
 */
public class CreateAccountLoginFragment extends BaseIdentityFragment {

    private EditText editText;
    private int mMessageId;
    private int mEditTextHintId;
    private BaseWizwardActivity.NextChecker mChecker;
    private TextView tvMessage;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_login, container, false);
        tvMessage = ((TextView) view.findViewById(R.id.et_username));
        editText = (EditText) view.findViewById(R.id.et_password);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_next, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ok) {
            Toast.makeText(getActivity(), "Dummy login", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public String check() {
        UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.function_not_yet_implenented);
        //return mChecker.check(editText);
        return null;
    }
}
