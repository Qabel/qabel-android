package de.qabel.qabelbox.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.BoxObjectComparators;
import de.qabel.qabelbox.storage.BoxExternalFile;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FilesViewHolder> {

    private final List<BoxObject> boxObjects;
    private OnItemClickListener onItemClickListener;
    private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    View emptyView, loadingView;

    public FilesAdapter(List<BoxObject> BoxObject) {

        boxObjects = BoxObject;
        registerAdapterDataObserver(observer);
    }

    class FilesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public final TextView mTextViewFolderName;
        public final TextView mTextViewFolderDetailsLeft;
        public final TextView mTextViewFolderDetailsMiddle;
        public final TextView mTextViewFolderDetailsRight;
        public final ImageView mImageView;
		public final ProgressBar mProgressBar;

        public FilesViewHolder(View v) {

            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mTextViewFolderName = (TextView) v.findViewById(R.id.textViewFolderName);
            mTextViewFolderDetailsLeft = (TextView) v.findViewById(R.id.textViewFolderDetailLeft);
            mTextViewFolderDetailsMiddle = (TextView) v.findViewById(R.id.textViewFolderDetailMiddle);
            mTextViewFolderDetailsRight = (TextView) v.findViewById(R.id.textViewFolderDetailRight);
            mImageView = (ImageView) v.findViewById(R.id.fileFolderIcon);
			mProgressBar = (ProgressBar) v.findViewById(R.id.fileFolderProgress);
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
			holder.mProgressBar.setVisibility(View.INVISIBLE);
        } else if (boxObject instanceof BoxExternalFile) {
            BoxExternalFile boxExternal = (BoxExternalFile) boxObject;
            holder.mImageView.setImageResource(R.drawable.ic_insert_drive_file_black);
            // TODO: Only show a part of the key identifier until owner name is implemented
            holder.mTextViewFolderDetailsLeft.setText("Owner: " + boxExternal.owner.getReadableKeyIdentifier().substring(0, 6));
            holder.mTextViewFolderDetailsRight.setText("");
			holder.mProgressBar.setVisibility(View.INVISIBLE);
		} else if (boxObject instanceof BoxFile) {
            BoxFile boxFile = (BoxFile) boxObject;
            holder.mTextViewFolderDetailsLeft.setText(formatModificationTime(boxFile));
            holder.mTextViewFolderDetailsRight.setText(FileUtils.byteCountToDisplaySize(boxFile.size));
            holder.mImageView.setImageResource(R.drawable.ic_insert_drive_file_black);
			holder.mProgressBar.setVisibility(View.INVISIBLE);
		} else if (boxObject instanceof BoxUploadingFile) {
			holder.mTextViewFolderDetailsLeft.setText(R.string.uploading);
			holder.mTextViewFolderDetailsRight.setText("");
			holder.mImageView.setImageResource(R.drawable.ic_cloud_upload_black_24dp);
			holder.mProgressBar.setVisibility(View.VISIBLE);
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

        if (position < boxObjects.size()) {
            return boxObjects.get(position);
        }
        return null;
    }

    public void sort() {
        Collections.sort(boxObjects, BoxObjectComparators.alphabeticOrderDirectoriesFirstIgnoreCase());
    }

    public void clear() {

        boxObjects.clear();
    }

    boolean loaded = false;

    void updateEmptyView() {

        if (loadingView != null) {
            int fileCount = boxObjects.size();
            if (fileCount > 0 || loadingView.getVisibility() != View.VISIBLE || loaded) {
                loadingView.setVisibility(View.GONE);
                loaded = true;
            }
            if (loaded) {
                emptyView.setVisibility(fileCount > 0 ? View.GONE : View.VISIBLE);
            }
        }
    }

    final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {

            super.onChanged();
            updateEmptyView();
        }
    };

    public void setEmptyView(@Nullable View emptyView, View loadingView) {

        this.emptyView = emptyView;
        this.loadingView = loadingView;
        updateEmptyView();
    }
}
