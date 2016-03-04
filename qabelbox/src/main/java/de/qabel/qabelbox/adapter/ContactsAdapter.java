package de.qabel.qabelbox.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.qabelbox.R;

/**
 * Contacts adapter provides data for a Contacts list in a RecyclerView. Allows to externally
 * set click actions on ViewHolders by setOnItemClickListener.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final List<Contact> mContacts;
    private OnItemClickListener onItemClickListener;
    private View emptyView;

    public ContactsAdapter(Contacts contacts) {

        mContacts = new ArrayList<>(contacts.getContacts());
        Collections.sort(mContacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {

                return lhs.getAlias().compareTo(rhs.getAlias());
            }
        });

        registerAdapterDataObserver(observer);
    }

    public Contact getContact(int position) {

        return mContacts.get(position);
    }

    class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTextViewContactName;
        public final TextView mTextViewContactDetails;
        public final ImageView mImageView;

        public ContactViewHolder(View v) {

            super(v);
            v.setOnClickListener(this);
            mTextViewContactName = (TextView) v.findViewById(R.id.textViewItemName);
            mTextViewContactDetails = (TextView) v.findViewById(R.id.textViewItemDetail);
            mImageView = (ImageView) v.findViewById(R.id.itemIcon);
        }

        @Override
        public void onClick(View view) {

            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener {

        void onItemClick(View view, int position);
    }

    /**
     * Sets the action to perform on item clicks
     *
     * @param onItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {

        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contacts, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {

        holder.mTextViewContactName.setText(mContacts.get(position).getAlias());
        holder.mTextViewContactDetails.setText(mContacts.get(position).getEcPublicKey().getReadableKeyIdentifier());
    }

    @Override
    public int getItemCount() {

        return mContacts.size();
    }

    void updateEmptyView() {

        if (emptyView != null) {

            emptyView.setVisibility(getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
    }

    final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {

            super.onChanged();
            updateEmptyView();
        }
    };

    public void setEmptyView(@Nullable View emptyView) {

        this.emptyView = emptyView;

        updateEmptyView();
    }
}
