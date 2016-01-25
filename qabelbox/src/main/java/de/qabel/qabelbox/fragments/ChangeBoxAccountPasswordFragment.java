package de.qabel.qabelbox.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.BaseWizwardActivity;
import de.qabel.qabelbox.helper.UIHelper;

/**
 * Created by danny on 19.01.16.
 */
public class ChangeBoxAccountPasswordFragment extends Fragment {
    private BaseWizwardActivity.NextChecker mChecker;
    private TextView tvMessage;
    Dialog mWaitDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_box_account_password, container, false);

        tvMessage = ((TextView) view.findViewById(R.id.et_name));
        setHasOptionsMenu(true);
        //tvMessage.setText(mMessageId);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ab_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_ok) {
            if (checkNewPassword()) {
                mWaitDialog = UIHelper.showWaitMessage(getActivity(), R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);
                sendChangePWRequest();
                return true;
            }

        }
        return false;
    }

    private boolean checkNewPassword() {

        return true;
    }

    private void sendChangePWRequest() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Toast.makeText(getActivity(), R.string.boxaccount_password_changed, Toast.LENGTH_LONG).show();
                mWaitDialog.dismiss();
                getActivity().onBackPressed();
            }

            @Override
            protected Void doInBackground(Void[] params) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
