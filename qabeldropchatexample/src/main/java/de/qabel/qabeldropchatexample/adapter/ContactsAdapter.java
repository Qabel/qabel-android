package de.qabel.qabeldropchatexample.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.qabel.IContact;
import de.qabel.qabeldropchatexample.R;


public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private List<IContact> mContacts;
    private OnContactSelectedListener onContactSelectedListener;

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView mTextView;
        private TextView textViewContactOwner;
        private OnItemClickListener onItemClickListener;
        public ViewHolder(View v, OnItemClickListener onItemClickListener) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.contact_text);
            mTextView.setOnClickListener(this);
            this.onItemClickListener = onItemClickListener;

            textViewContactOwner = (TextView) v.findViewById(R.id.textViewContactOwner);
            textViewContactOwner.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemClickListener.onItemClick(view, getPosition());
        }
    }

    public ContactsAdapter(final ArrayList<IContact> contacts, OnContactSelectedListener onContactSelectedListener) {
       this.mContacts = contacts;
       this.onContactSelectedListener = onContactSelectedListener;
    }

    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_text_view, parent, false);

        ViewHolder vh = new ViewHolder(v, new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                onContactSelectedListener.onContactSelected(
                        mContacts.get(position));
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(mContacts.get(position).getAlias());
        holder.textViewContactOwner.setText(mContacts.get(position).getContactOwnerAlias());
    }

    @Override
    public int getItemCount() {
        if (mContacts == null) {
            return 0;
        }
        return mContacts.size();
    }

    public interface OnContactSelectedListener {
        void onContactSelected(IContact contact);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}