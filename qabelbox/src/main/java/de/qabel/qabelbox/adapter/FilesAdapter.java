package de.qabel.qabelbox.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.helper.BoxObjectComparators;
import de.qabel.qabelbox.helper.Formatter;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxExternalFile;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FilesViewHolder> {

    private final List<BoxObject> boxObjects;
    private final Map<String, BoxObject> boxObjectsByName;
    private final Identity currentIdentity;
    private final Context context;
    private final LocalQabelService mService;
    private OnItemClickListener onItemClickListener;
    private View emptyView;
    private View loadingView;

    public FilesAdapter(List<BoxObject> BoxObject) {

        boxObjects = BoxObject;
        boxObjectsByName = new HashMap<>();
        for (BoxObject boxObject : boxObjects) {
            boxObjectsByName.put(boxObject.name, boxObject);
        }
        registerAdapterDataObserver(observer);
        currentIdentity = QabelBoxApplication.getInstance().getService().getActiveIdentity();
        context = QabelBoxApplication.getInstance().getApplicationContext();
        mService = QabelBoxApplication.getInstance().getService();
    }

    class FilesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public final TextView mTextViewFolderName;
        public final TextView mTextViewFolderDetailsLeft;
        public final TextView mTextViewFolderDetailsRight;
        public final ImageView mImageView;
        public final ProgressBar mProgressBar;
        public final View mSeparator;

        public final View detailsRow;

        public FilesViewHolder(View v) {

            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mTextViewFolderName = (TextView) v.findViewById(R.id.textViewFolderName);
            mTextViewFolderDetailsLeft = (TextView) v.findViewById(R.id.textViewFolderDetailLeft);
            mTextViewFolderDetailsRight = (TextView) v.findViewById(R.id.textViewFolderDetailRight);
            mImageView = (ImageView) v.findViewById(R.id.fileFolderIcon);
            mProgressBar = (ProgressBar) v.findViewById(R.id.fileFolderProgress);
            mSeparator = v.findViewById(R.id.separator);
            detailsRow = v.findViewById(R.id.second_row);
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

    public int getBoxObjectIcon(BoxObject object) {
        if (object instanceof BoxUploadingFile) {
            return R.drawable.cloud_upload;
        } else if (object instanceof BoxFile) {
            return R.drawable.file;
        }
        if (object.name.equals(BoxFolder.RECEIVED_SHARE_NAME)) {
            return R.drawable.folder_account;
        }
        return R.drawable.folder;
    }

    @Override
    public void onBindViewHolder(FilesViewHolder holder, int position) {

        BoxObject boxObject = boxObjects.get(position);
        holder.mTextViewFolderName.setText(boxObject.name);
        holder.mTextViewFolderDetailsRight.setVisibility(View.INVISIBLE);
        holder.mImageView.setImageResource(getBoxObjectIcon(boxObject));
        holder.mSeparator.setVisibility(View.GONE);
        holder.detailsRow.setVisibility(View.VISIBLE);
//        if (boxObject.getShareCount() > 0) {
//            holder.mTextViewFolderDetailsLeft.setText(context.getResources().getQuantityString(
//                    R.plurals.sharedWith, boxObject.getShareCount(), boxObject.getShareCount()));
//        }
        StringBuilder left = new StringBuilder();
        if (boxObject instanceof BoxFolder) {
            if (boxObject.name.equals(BoxFolder.RECEIVED_SHARE_NAME)) {
                holder.mSeparator.setVisibility(View.VISIBLE);
            }
            // Always set all ViewHolder fields, otherwise recycled views contain wrong data
            holder.mProgressBar.setVisibility(View.INVISIBLE);
            holder.detailsRow.setVisibility(View.GONE);
        } else if (boxObject instanceof BoxFile) {
            BoxFile boxFile = (BoxFile) boxObject;

            holder.mTextViewFolderDetailsRight.setText(formatModificationTime(boxFile));
            holder.mTextViewFolderDetailsRight.setVisibility(View.VISIBLE);

            if (boxObject instanceof BoxExternalFile) {
                BoxExternalFile boxExternal = (BoxExternalFile) boxObject;
                String owner = getOwner(boxExternal);
                left.append(context.getString(R.string.filebrowser_file_is_shared_from).replace("%1", owner));
                left.append(" - ");
            } else if (boxFile.isShared()) {
                left.append(context.getString(R.string.filebrowser_file_is_shared_to_other));
                left.append(" - ");
            }
            left.append(FileUtils.byteCountToDisplaySize(boxFile.size));
            holder.mProgressBar.setVisibility(View.INVISIBLE);
        } else if (boxObject instanceof BoxUploadingFile) {
            left.append(context.getString(R.string.uploading));
            holder.mProgressBar.setVisibility(View.VISIBLE);
        }
        holder.mTextViewFolderDetailsLeft.setText(left.toString());
    }

    /**
     * return file owner name or key if not in contact list
     *
     * @param boxExternal
     * @return
     */
    private String getOwner(BoxExternalFile boxExternal) {

        Contact contact = mService.getContacts().getByKeyIdentifier(boxExternal.getOwner().getReadableKeyIdentifier());
        if (contact != null)
            return contact.getAlias();
        return boxExternal.owner.getReadableKeyIdentifier().substring(0, 6);
    }

    private String formatModificationTime(BoxFile boxFile) {
        return Formatter.formatDateTimeString(boxFile.mtime * 1000);
    }

    @Override
    public int getItemCount() {

        return boxObjects.size();
    }

    public boolean add(BoxObject boxObject) {

        boxObjectsByName.put(boxObject.name, boxObject);
        return boxObjects.add(boxObject);
    }

    public boolean remove(BoxObject boxObject) {

        boxObjectsByName.remove(boxObject.name);
        return boxObjects.remove(boxObject);
    }

    public boolean remove(String name) {

        BoxObject toRemove = boxObjectsByName.remove(name);
        return boxObjects.remove(toRemove);
    }

    public BoxObject get(int position) {

        if (position < boxObjects.size()) {
            return boxObjects.get(position);
        }
        return null;
    }

    public BoxObject get(String name) {

        return boxObjectsByName.get(name);
    }

    public void sort() {

        Collections.sort(boxObjects, BoxObjectComparators.alphabeticOrderDirectoriesFirstIgnoreCase());
    }

    public void clear() {

        boxObjectsByName.clear();
        boxObjects.clear();
    }

    public boolean containsEqual(BoxObject object) {

        for (BoxObject boxObject : boxObjects) {
            if (object.equals(boxObject)) {
                return true;
            }
        }
        return false;
    }

    private boolean loaded = false;

    private void updateEmptyView() {

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

    private final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
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
