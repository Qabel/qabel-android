package de.qabel.qabelbox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.adapter.JSONLicencesAdapter;

/**
 * Created by r-hold on 29.02.16.
 */
public class AboutLicencesFragment extends BaseFragment {

    static final String TAG = "AboutLicencesFragment";
    RecyclerView licensesList;

    public static AboutLicencesFragment newInstance() {
        AboutLicencesFragment fragment=new AboutLicencesFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aboutlicences, container, false);
    }

    public String readUTF8FromAssets(String filename) throws IOException {

        String content = null;
        try {
            InputStream is = getActivity().getAssets().open("yourfilename.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            content = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return content;
    }

    public JSONObject readJSONFromAssets(String filename) throws IOException, JSONException {
        return new JSONObject(readUTF8FromAssets(filename));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        licensesList = (RecyclerView) view.findViewById(R.id.about_licences_list);
        try {
            licensesList.setAdapter(new JSONLicencesAdapter(getActivity(), readJSONFromAssets("licenceinfo.json")));
        } catch (Exception e) {
            Log.e(TAG, "Could not read licence JSON: " + e.getMessage());
        }
    }


}
