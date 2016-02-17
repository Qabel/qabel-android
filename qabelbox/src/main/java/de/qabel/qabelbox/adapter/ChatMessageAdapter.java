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
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.model.ChatMessageItem;
import de.qabel.qabelbox.helper.Formatter;
import de.qabel.qabelbox.storage.ChatMessagesDataBase;

/**
 * Contacts adapter provides data for a Contacts list in a RecyclerView. Allows to externally
 * set click actions on ViewHolders by setOnItemClickListener.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ContactViewHolder> {

    private List<ChatMessageItem> mMessages = null;
    private OnItemClickListener onItemClickListener;
    private View emptyView;

    public ChatMessageAdapter(ArrayList<ChatMessagesDataBase.ChatMessageDatabaseItem> allMessages, Contact contact) {

        mMessages = new ArrayList<>();
        for (ChatMessageItem message : allMessages) {
            //if (message.getSenderKey().equals(contact.getEcPublicKey()))

            {
                mMessages.add(message);
            }
        }
        Collections.sort(mMessages, new Comparator<ChatMessageItem>() {
            @Override
            public int compare(ChatMessageItem o1, ChatMessageItem o2) {

                return (o1.getTime() > o2.getTime() ? 1 : (o1.getTime() == o2.getTime() ? 0 : -1));
            }
        });

        registerAdapterDataObserver(observer);
    }

    public ChatMessageItem getMessage(int position) {

        return mMessages.get(position);
    }

    class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTextViewContactName;
        public final TextView mTextViewContactDetails;
        public final ImageView mImageView;
        public final View mBg;

        public ContactViewHolder(View v) {

            super(v);
            v.setOnClickListener(this);
            mBg = v;
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

        ChatMessageItem message = mMessages.get(position);
        holder.mTextViewContactName.setText(Formatter.formatDateTimeShort(message.getTime()));
        ChatMessageItem.Type payload = message.getModelObject();
        if (payload != null && payload == ChatMessageItem.Type.BOX_MESSAGE) {
            holder.mTextViewContactDetails.setText(message.getData().getMessage());
        } else {
            //@todo is other type as string
            holder.mTextViewContactDetails.setText(message.getData().getMessage());
        }
        holder.mBg.setBackgroundResource(R.drawable.chat_in_message_bg);
    }

    @Override
    public int getItemCount() {

        return mMessages.size();
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
