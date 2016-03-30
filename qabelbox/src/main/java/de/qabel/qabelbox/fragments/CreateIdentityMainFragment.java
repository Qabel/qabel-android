package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import de.qabel.core.config.Identity;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.activities.CreateIdentityActivity;
import de.qabel.qabelbox.config.IdentityExportImport;
import de.qabel.qabelbox.helper.UIHelper;
import de.qabel.qabelbox.services.LocalQabelService;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class CreateIdentityMainFragment extends BaseIdentityFragment implements OnClickListener {
    private Button mCreateIdentity;
    private Button mImportIdentity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(layout.fragment_create_identity_main, container, false);
        mCreateIdentity = (Button) view.findViewById(id.bt_create_identity);
        mImportIdentity = (Button) view.findViewById(id.bt_import_identity);
        mCreateIdentity.setOnClickListener(this);
        mImportIdentity.setOnClickListener(this);
        return view;
    }

    @Override
    public String check() {
        return null;
    }

    @Override
    public void onClick(View v) {
        if (v == mCreateIdentity) {
            mActivity.handleNextClick();
        }
        if (v == mImportIdentity) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, CreateIdentityActivity.REQUEST_CODE_IMPORT_IDENTITY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == CreateIdentityActivity.REQUEST_CODE_IMPORT_IDENTITY && resultCode == Activity.RESULT_OK) {
            importIdentity(mActivity, resultData);
        }
    }

    public boolean importIdentity(Activity activity, Intent resultData) {
        if (resultData != null) {
            Uri uri = resultData.getData();
            try (InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                Identity importedIdentity = IdentityExportImport.parseIdentity(stringBuilder.toString());

                LocalQabelService mService = QabelBoxApplication.getInstance().getService();
                mService.addIdentity(importedIdentity);
                if (mService.getActiveIdentity() == null) {
                    mService.setActiveIdentity(importedIdentity);
                }

                if (activity instanceof CreateIdentityActivity) {
                    CreateIdentityActivity createActivity = (CreateIdentityActivity) activity;
                    createActivity.activityResult = Activity.RESULT_OK;
                    createActivity.setCreatedIdentity(importedIdentity);
                    createActivity.completeWizard();
                }
                return true;
            } catch (IOException | JSONException | URISyntaxException | QblDropInvalidURL e) {
                UIHelper.showDialogMessage(activity, string.dialog_headline_info, string.cant_read_identity);
            }
        }
        return false;
    }
}


