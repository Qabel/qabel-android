package de.qabel.qabelbox.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.qabel.core.config.Contact;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.adapter.ContactsAdapter.ContactViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Contacts adapter provides data for a Contacts list in a RecyclerView. Allows to externally
 * set click actions on ViewHolders by setOnItemClickListener.
 */
public class ContactsAdapter extends Adapter<ContactViewHolder> {
    private final List<ContactAdapterItem> mContacts;
    private OnItemClickListener onItemClickListener, onItemLongClickListener;
    private View emptyView;

    public ContactsAdapter(ArrayList<ContactAdapterItem> contacts) {
        mContacts = contacts;
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

    class ContactViewHolder extends ViewHolder implements OnClickListener, OnLongClickListener {
        public final TextView mTextViewContactName;
        public final TextView mTextViewContactDetails;
        public final ImageView mImageView;
        private final View mNewMessageView;

        public ContactViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mTextViewContactName = (TextView) v.findViewById(id.textViewItemName);
            mTextViewContactDetails = (TextView) v.findViewById(id.textViewItemDetail);
            mNewMessageView = v.findViewById(id.newMessageIndicator);
            mImageView = (ImageView) v.findViewById(id.itemIcon);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemClick(view, getAdapterPosition());
                return true;
            }
            return false;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    /**
     * Sets the action to perform on item clicks
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener, OnItemClickListener onItemLongClickListener) {
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layout.item_contacts, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        ContactAdapterItem item = mContacts.get(position);
        holder.mTextViewContactName.setText(item.getAlias());
        holder.mTextViewContactDetails.setText(item.getEcPublicKey().getReadableKeyIdentifier());
        if (item.hasNewMessages) {
            holder.mNewMessageView.setVisibility(View.VISIBLE);
        } else {
            holder.mNewMessageView.setVisibility(View.INVISIBLE);
        }

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

    final AdapterDataObserver observer = new AdapterDataObserver() {
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
