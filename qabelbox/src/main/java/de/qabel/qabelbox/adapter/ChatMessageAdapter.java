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
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.helper.Formatter;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ContactViewHolder> {

    private final String TAG = getClass().getSimpleName();
    private final String contactPublicKey;
    private List<ChatMessageItem> mMessages = null;
    private OnItemClickListener onItemClickListener;
    private View emptyView;

    public ChatMessageAdapter(ArrayList<ChatMessageItem> allMessages, Contact contact) {

        mMessages = new ArrayList<>();

        contactPublicKey = contact.getEcPublicKey().getReadableKeyIdentifier().toString();
        for (ChatMessageItem message : allMessages) {
            if (contactPublicKey.equals(message.getSenderKey()) || contactPublicKey.equals(message.getReceiverKey()))

            {
                mMessages.add(message);
            }
        }
        Collections.sort(mMessages, new Comparator<ChatMessageItem>() {
            @Override
            public int compare(ChatMessageItem o1, ChatMessageItem o2) {


                return (o1.getTime() > o2.getTime() ? -1 : (o1.getTime() == o2.getTime() ? 0 : 1));
            }
        });

        registerAdapterDataObserver(observer);
    }

    public ChatMessageItem getMessage(int position) {

        return mMessages.get(position);
    }

    class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView tvDate;
        public final TextView tvText;
        public final TextView mLink;
        public final ImageView mImageView;
        public final View mBg;

        public ContactViewHolder(View v) {

            super(v);
            v.setOnClickListener(this);
            mBg = v.findViewById(R.id.chatTextLayout);
            tvDate = (TextView) v.findViewById(R.id.tvDate);
            tvText = (TextView) v.findViewById(R.id.tvText);
            mLink = (TextView) v.findViewById(R.id.tvLink);
            mImageView = (ImageView) v.findViewById(R.id.itemIcon);
        }

        @Override
        public void onClick(View view) {

            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(mMessages.get( getAdapterPosition()));
            }
        }
    }

    public interface OnItemClickListener {

        void onItemClick(ChatMessageItem item);
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
                .inflate(R.layout.item_chat_message, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {

        ChatMessageItem message = mMessages.get(position);
        holder.tvDate.setText(Formatter.formatDateTimeShort(message.getTime()));
        ChatMessageItem.MessagePayload messageData = message.getData();
        if (messageData != null && messageData instanceof ChatMessageItem.TextMessagePayload) {
            holder.tvText.setText(((ChatMessageItem.TextMessagePayload) messageData).getMessage());
            holder.mLink.setVisibility(View.GONE);
        } else if (messageData != null && messageData instanceof ChatMessageItem.ShareMessagePayload) {
            holder.tvText.setText(((ChatMessageItem.ShareMessagePayload) messageData).getMessage());
            holder.mLink.setText(((ChatMessageItem.ShareMessagePayload) messageData).getURL());
            holder.mLink.setVisibility(View.VISIBLE);
        }
        if (contactPublicKey.equals(message.getSenderKey())) {
            holder.mBg.setBackgroundResource(R.drawable.chat_in_message_bg);
        } else {
            holder.mBg.setBackgroundResource(R.drawable.chat_out_message_bg);
        }
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
