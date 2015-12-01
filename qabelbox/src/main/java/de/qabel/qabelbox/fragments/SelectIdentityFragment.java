package de.qabel.qabelbox.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;

/**
 * Activities that contain this fragment must implement the
 * {@link SelectIdentityFragment.SelectIdentityListener} interface
 * to handle interaction events.
 */
public class SelectIdentityFragment extends Fragment {

    private static final String ARG_IDENTITIES = "Identities";
    private Spinner spinnerSelectIdentity;
    private Button buttonSelectIdentity;
    private Identities identities;
    private SelectIdentityListener mListener;

    class IdentityAdapter extends ArrayAdapter<Identity> {

        private Context context;
        private Identity[] values;

        public IdentityAdapter(Context context, int resource, Identity[] values) {
            super(context, resource, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private TextView getCustomView(int position, View convertView, ViewGroup parent) {
            View item;
            if (convertView != null) {
                item = convertView;
            } else {
                LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                item = layoutInflater.inflate(R.layout.spinner_item, parent, false);
            }
            TextView textView = (TextView) item.findViewById(R.id.textSpinner);
            textView.setText(values[position].getAlias());
            return textView;
        }
    }

    public static SelectIdentityFragment newInstance(Identities identities) {
        SelectIdentityFragment fragment = new SelectIdentityFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDENTITIES, identities);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            identities = (Identities) arguments.getSerializable(ARG_IDENTITIES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_select_identity, container, false);

        spinnerSelectIdentity = (Spinner) view.findViewById(R.id.spinnerSelectIdentity);
        buttonSelectIdentity = (Button) view.findViewById(R.id.buttonIdentitySelect);

        List<Identity> identityList = new ArrayList<>(identities.getIdentities());

        ArrayAdapter<Identity> spinnerArrayAdapter =
                new IdentityAdapter(getActivity().getApplicationContext(),
                        R.layout.spinner_item, identityList.toArray(new Identity[0]));
        spinnerSelectIdentity.setAdapter(spinnerArrayAdapter);

        buttonSelectIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.selectIdentity((Identity) spinnerSelectIdentity.getSelectedItem());
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SelectIdentityListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SelectIdentityListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface SelectIdentityListener {
        void selectIdentity(Identity identity);
        void startAddIdentity();
    }
}
