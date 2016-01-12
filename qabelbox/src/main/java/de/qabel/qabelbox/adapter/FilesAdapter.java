package de.qabel.qabelbox.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import org.apache.commons.io.FileUtils;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.qabel.qabelbox.storage.BoxExternal;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.R;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FilesViewHolder> {
    private final List<BoxObject> boxObjects;
    private OnItemClickListener onItemClickListener;
    private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);


    public FilesAdapter(List<BoxObject> BoxObject) {
        boxObjects = BoxObject;
    }

    class FilesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public final TextView mTextViewFolderName;
        public final TextView mTextViewFolderDetailsLeft;
        public final TextView mTextViewFolderDetailsMiddle;
        public final TextView mTextViewFolderDetailsRight;
        public final ImageView mImageView;

        public FilesViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mTextViewFolderName = (TextView) v.findViewById(R.id.textViewFolderName);
            mTextViewFolderDetailsLeft = (TextView) v.findViewById(R.id.textViewFolderDetailLeft);
            mTextViewFolderDetailsMiddle = (TextView) v.findViewById(R.id.textViewFolderDetailMiddle);
            mTextViewFolderDetailsRight = (TextView) v.findViewById(R.id.textViewFolderDetailRight);
            mImageView = (ImageView) v.findViewById(R.id.fileFolderIcon);
        }

        @Override
        public void onClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemLockClick(view, getAdapterPosition());
                return true;
            }
            return false;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onItemLockClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public FilesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_files, parent, false);
        return new FilesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FilesViewHolder holder, int position) {
        BoxObject boxObject = boxObjects.get(position);
        holder.mTextViewFolderName.setText(boxObject.name);
//        if (boxObject.getShareCount() > 0) {
//            holder.mTextViewFolderDetailsLeft.setText(context.getResources().getQuantityString(
//                    R.plurals.sharedWith, boxObject.getShareCount(), boxObject.getShareCount()));
//        }
        if (boxObject instanceof BoxFolder) {
            holder.mImageView.setImageResource(R.drawable.ic_folder_black);
            // Always set all ViewHolder fields, otherwise recycled views contain wrong data
            holder.mTextViewFolderDetailsLeft.setText("");
            holder.mTextViewFolderDetailsRight.setText("");
        } else if (boxObject instanceof BoxExternal) {
            BoxExternal boxExternal = (BoxExternal) boxObject;
            holder.mImageView.setImageResource(R.drawable.ic_folder_shared_black);
            // TODO: Only show a part of the key identifier until owner name is implemented
            holder.mTextViewFolderDetailsLeft.setText("Owner: " + boxExternal.owner.getReadableKeyIdentifier().substring(0, 6));
            holder.mTextViewFolderDetailsRight.setText("");
        }
        else if (boxObject instanceof BoxFile) {
            BoxFile boxFile = (BoxFile) boxObject;
            holder.mTextViewFolderDetailsLeft.setText(formatModificationTime(boxFile));
            holder.mTextViewFolderDetailsRight.setText(FileUtils.byteCountToDisplaySize(boxFile.size));
            holder.mImageView.setImageResource(R.drawable.ic_insert_drive_file_black);
        }
        holder.mImageView.setAlpha(0.8f);
    }

    private String formatModificationTime(BoxFile boxFile) {
        return dateFormat.format(new Date(boxFile.mtime * 1000));
    }

    @Override
    public int getItemCount() {
        return boxObjects.size();
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

    public void clear() {
        boxObjects.clear();
    }
}
