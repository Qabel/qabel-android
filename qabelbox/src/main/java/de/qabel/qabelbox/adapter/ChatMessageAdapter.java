package de.qabel.qabelbox.adapter;

import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.qabel.core.config.Contact;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatMessageItem.MessagePayload;
import de.qabel.qabelbox.chat.ChatMessageItem.ShareMessagePayload;
import de.qabel.qabelbox.chat.ChatMessageItem.TextMessagePayload;
import de.qabel.qabelbox.helper.Formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatMessageAdapter extends BaseAdapter {
    private final String TAG = getClass().getSimpleName();
    private final String contactPublicKey;
    private List<ChatMessageItem> mMessages;
    private OnItemClickListener onItemClickListener;
    private View emptyView;

    public ChatMessageAdapter(ArrayList<ChatMessageItem> allMessages, Contact contact) {
        mMessages = new ArrayList<>();

        contactPublicKey = contact.getEcPublicKey().getReadableKeyIdentifier().toString();
        for (ChatMessageItem message : allMessages) {
            if (contactPublicKey.equals(message.getSenderKey()) || contactPublicKey.equals(message.getReceiverKey())) {
                mMessages.add(message);
            }
        }
        Collections.sort(mMessages, new Comparator<ChatMessageItem>() {
            @Override
            public int compare(ChatMessageItem o1, ChatMessageItem o2) {
                return o1.getTime() > o2.getTime() ? 1 : o1.getTime() == o2.getTime() ? 0 : -1;
            }
        });

        registerDataSetObserver(observer);
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
            if (convertView == null || convertView.getId() == id.checkMessageOut) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(layout.item_chat_message_in, parent, false);
            }
        } else {
            if (convertView == null || convertView.getId() == id.checkMessageIn) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(layout.item_chat_message_out, parent, false);
            }
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
        return getMessage(position).getSenderKey().equals(contactPublicKey) ? INCOMING : OUTGOING;
    }

    class ContactViewHolder extends ViewHolder implements OnClickListener {
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
            mBg = v.findViewById(id.chatTextLayout);
            tvDate = (TextView) v.findViewById(id.tvDate);
            tvText = (TextView) v.findViewById(id.tvText);
            mLink = (TextView) v.findViewById(id.tvLink);
            mImageView = (ImageView) v.findViewById(id.itemIcon);
            fileContainer = v.findViewById(id.messageFileContainer);
            applyData(message);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(messageItem);
            }
        }

        public void applyData(ChatMessageItem message) {
            messageItem = message;
            tvDate.setText(Formatter.formatDateTimeString(message.getTime()));
            MessagePayload messageData = message.getData();
            if (messageData != null && messageData instanceof TextMessagePayload) {
                tvText.setText(((TextMessagePayload) messageData).getMessage());
                fileContainer.setVisibility(View.GONE);
            } else if (messageData != null && messageData instanceof ShareMessagePayload) {
                tvText.setText(((ShareMessagePayload) messageData).getMessage());
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
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

	/*@Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_chat_message_out, parent, false);
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
