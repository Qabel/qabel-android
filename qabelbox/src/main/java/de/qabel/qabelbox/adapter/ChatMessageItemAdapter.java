package de.qabel.qabelbox.adapter;

import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

public class ChatMessageItemAdapter extends BaseAdapter {

    private final String TAG = getClass().getSimpleName();
    private String contactPublicKey;
    private List<ChatMessageItem> mMessages = null;
    private OnItemClickListener onItemClickListener;
    private View emptyView;

    public ChatMessageItemAdapter(ArrayList<ChatMessageItem> allMessages, Contact contact) {

        mMessages = new ArrayList<>();
        registerDataSetObserver(observer);
        setMessages(allMessages, contact);
    }

    public void setMessages(ArrayList<ChatMessageItem> messages, Contact contact) {
        contactPublicKey = contact.getEcPublicKey().getReadableKeyIdentifier();
        mMessages.clear();

        for (ChatMessageItem message : messages) {
            if (contactPublicKey.equals(message.getSenderKey()) || contactPublicKey.equals(message.getReceiverKey())) {
                mMessages.add(message);
            }
        }
        Collections.sort(mMessages, new Comparator<ChatMessageItem>() {
            @Override
            public int compare(ChatMessageItem o1, ChatMessageItem o2) {
                return (o1.getTime() > o2.getTime() ? 1 : (o1.getTime() == o2.getTime() ? 0 : -1));
            }
        });

    }


    public ChatMessageItem getMessage(int position) {

        return mMessages.get(position);
    }

    private static final int INCOMING = 0;
    private static final int OUTGOING = 1;

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mMessages.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == INCOMING) {
            if (convertView == null || convertView.getId() == R.id.checkMessageOut)
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_message_in, parent, false);
        } else {
            if (convertView == null || convertView.getId() == R.id.checkMessageIn)
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.chat_message_out, parent, false);
        }

        ChatMessageItem message = mMessages.get(position);
        if (convertView.getTag() != null) {
            ((ContactViewHolder) convertView.getTag()).applyData(message);
        } else {
            convertView.setTag(new ContactViewHolder(convertView, message));
        }

        return convertView;
    }


    @Override
    public int getItemViewType(int position) {
        return this.getMessage(position).getSenderKey().equals(contactPublicKey) ? INCOMING : OUTGOING;
    }

    class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ChatMessageItem messageItem;

        public final TextView tvDate;
        public final TextView tvText;
        public final TextView mLink;
        public final ImageView mImageView;
        public final View mBg;
        public final View fileContainer;

        public ContactViewHolder(View v, ChatMessageItem message) {
            super(v);
            v.setOnClickListener(this);
            mBg = v.findViewById(R.id.chatTextLayout);
            tvDate = (TextView) v.findViewById(R.id.tvDate);
            tvText = (TextView) v.findViewById(R.id.tvText);
            mLink = (TextView) v.findViewById(R.id.tvLink);
            mImageView = (ImageView) v.findViewById(R.id.itemIcon);
            fileContainer = v.findViewById(R.id.messageFileContainer);
            applyData(message);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(messageItem);
            }
        }

        public void applyData(ChatMessageItem message) {
            this.messageItem = message;
            tvDate.setText(Formatter.formatDateTimeString(message.getTime()));
            ChatMessageItem.MessagePayload messageData = message.getData();
            if (messageData != null && messageData instanceof ChatMessageItem.TextMessagePayload) {
                tvText.setText(((ChatMessageItem.TextMessagePayload) messageData).getMessage());
                fileContainer.setVisibility(View.GONE);
            } else if (messageData != null && messageData instanceof ChatMessageItem.ShareMessagePayload) {
                tvText.setText(((ChatMessageItem.ShareMessagePayload) messageData).getMessage());
                //holder.mLink.setText(((ChatMessageItem.ShareMessagePayload) messageData).getURL());
                fileContainer.setVisibility(View.VISIBLE);
            }
            mBg.forceLayout();
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

	/*@Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.chat_message_out, parent, false);
		return new ContactViewHolder(v);
	}*/

    void updateEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(getCount() > 0 ? View.GONE : View.VISIBLE);
        }
    }

    final DataSetObserver observer = new DataSetObserver() {
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
