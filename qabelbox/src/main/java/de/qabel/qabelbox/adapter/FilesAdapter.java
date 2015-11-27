package de.qabel.qabelbox.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.Collections;
import java.util.List;

import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.R;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FilesViewHolder> {
    private final List<BoxObject> boxObjects;
    private OnItemClickListener onItemClickListener;
    private int longClickedPosition;

    public FilesAdapter(List<BoxObject> BoxObject) {
        boxObjects = BoxObject;
    }

    class FilesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public final TextView mTextViewFolderName;
        public final TextView mTextViewFolderDetails;
        public final ImageView mImageView;

        public FilesViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mTextViewFolderName = (TextView) v.findViewById(R.id.textViewFolderName);
            mTextViewFolderDetails = (TextView) v.findViewById(R.id.textViewFolderDetail);
            mImageView = (ImageView) v.findViewById(R.id.fileFolderIcon);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            longClickedPosition = getAdapterPosition();
            return false;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public FilesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.files_item, parent, false);
        return new FilesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FilesViewHolder holder, int position) {
        BoxObject boxObject = boxObjects.get(position);
        holder.mTextViewFolderName.setText(boxObject.name);
//        if (boxObject.getShareCount() > 0) {
//            holder.mTextViewFolderDetails.setText(context.getResources().getQuantityString(
//                    R.plurals.sharedWith, boxObject.getShareCount(), boxObject.getShareCount()));
//        }
        if (boxObject instanceof BoxFolder) {
            holder.mImageView.setImageResource(R.drawable.ic_folder_black_24dp);
        }
        else {
            holder.mImageView.setImageResource(R.drawable.ic_insert_drive_file_black_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return boxObjects.size();
    }

    public int getLongClickedPosition() {
        return longClickedPosition;
    }

    public boolean add(BoxObject boxObject) {
        return boxObjects.add(boxObject);
    }

    public BoxObject get(int position) {
        return boxObjects.get(position);
    }

    public void sort() {
        Collections.sort(boxObjects);
    }
}
