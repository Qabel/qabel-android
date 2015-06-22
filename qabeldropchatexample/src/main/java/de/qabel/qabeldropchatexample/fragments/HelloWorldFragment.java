package de.qabel.qabeldropchatexample.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.qabel.IContact;
import de.qabel.qabeldropchatexample.R;


public class HelloWorldFragment extends Fragment {
    private static final String ARG_CONTACT = "contact";
    public static final String HELLO_WORLD_TYPE = "hello_world";

    private IContact mContact;
    private Button buttonSend;
    private EditText input;
    private TextView messageView;
    private String text;

    private OnSendMessageInterface mListener;

    public static HelloWorldFragment newInstance(IContact contact) {
        HelloWorldFragment fragment = new HelloWorldFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT, contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mContact = (IContact) getArguments().getSerializable(ARG_CONTACT);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_hello_world, container, false);

        buttonSend = (Button) view.findViewById(R.id.button_send);
        input = (EditText) view.findViewById(R.id.edit_text_input);
        messageView = (TextView) view.findViewById(R.id.message_view);
        messageView.setText(text);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onSendDropMessage(mContact, input.getText().toString(), HELLO_WORLD_TYPE);
                input.setText("");
                messageView.setText(text);
            }
        });

        return view;
    }

    public void setMessageText(String message) {
        text = message;

        if (messageView != null) {
            messageView.setText(message);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSendMessageInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSendMessageInterface {
        void onSendDropMessage(IContact contact, String message, String type);
    }
}
