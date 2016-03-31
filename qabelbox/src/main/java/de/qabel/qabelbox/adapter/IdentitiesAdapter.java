package de.qabel.qabelbox.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Identities adapter provides data for a Identity list in a RecyclerView. Allows to externally
 * set click actions on ViewHolders by setOnItemClickListener.
 */
public class IdentitiesAdapter extends RecyclerView.Adapter<IdentitiesAdapter.IdentityViewHolder> {
    private final List<Identity> mIdentities;
    private OnItemClickListener onItemClickListener;

    public IdentitiesAdapter(Identities identities) {
        mIdentities = new ArrayList<>(identities.getIdentities());
    }

    class IdentityViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView mTextViewIdentityName;
        public final TextView mTextViewIdentityDetails;
        public final ImageView mImageView;

        public IdentityViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            mTextViewIdentityName = (TextView) v.findViewById(R.id.textViewItemName);
            mTextViewIdentityDetails = (TextView) v.findViewById(R.id.textViewItemDetail);
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
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public IdentityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_identities, parent, false);
        return new IdentityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(IdentityViewHolder holder, int position) {
        Identity item = mIdentities.get(position);
        holder.mTextViewIdentityName.setText(item.getAlias());
        holder.mTextViewIdentityDetails.setText(item.getEcPublicKey().getReadableKeyIdentifier());
    }

    @Override
    public int getItemCount() {
        return mIdentities.size();
    }

    public Identity get(int position) {
        return mIdentities.get(position);
    }

    public boolean remove(Identity identity) {
        return mIdentities.remove(identity);
    }

    public void sort() {
        Collections.sort(mIdentities, new Comparator<Identity>() {
            @Override
            public int compare(Identity lhs, Identity rhs) {
                return lhs.getAlias().compareTo(rhs.getAlias());
            }
        });
    }
}
