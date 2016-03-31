package de.qabel.qabelbox.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.R.drawable;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.layout;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.adapter.FilesAdapter.FilesViewHolder;
import de.qabel.qabelbox.helper.BoxObjectComparators;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.*;
import org.apache.commons.io.FileUtils;

import java.text.DateFormat;
import java.util.*;

public class FilesAdapter extends Adapter<FilesViewHolder> {

    private final List<BoxObject> boxObjects;
    private final Map<String, BoxObject> boxObjectsByName;
    private final Identity currentIdentity;
    private final Context context;
    private final LocalQabelService mService;
    private OnItemClickListener onItemClickListener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
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

    class FilesViewHolder extends ViewHolder implements OnClickListener, OnLongClickListener {

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
            mTextViewFolderName = (TextView) v.findViewById(id.textViewFolderName);
            mTextViewFolderDetailsLeft = (TextView) v.findViewById(id.textViewFolderDetailLeft);
            mTextViewFolderDetailsMiddle = (TextView) v.findViewById(id.textViewFolderDetailMiddle);
            mTextViewFolderDetailsRight = (TextView) v.findViewById(id.textViewFolderDetailRight);
            mImageView = (ImageView) v.findViewById(id.fileFolderIcon);
            mProgressBar = (ProgressBar) v.findViewById(id.fileFolderProgress);
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
                .inflate(layout.item_files, parent, false);
        return new FilesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FilesViewHolder holder, int position) {

        BoxObject boxObject = boxObjects.get(position);
        holder.mTextViewFolderName.setText(boxObject.name);
        holder.mTextViewFolderDetailsRight.setVisibility(View.VISIBLE);
        //        if (boxObject.getShareCount() > 0) {
        //            holder.mTextViewFolderDetailsLeft.setText(context.getResources().getQuantityString(
        //                    R.plurals.sharedWith, boxObject.getShareCount(), boxObject.getShareCount()));
        //        }
        if (boxObject instanceof BoxFolder) {
            holder.mImageView.setImageResource(drawable.ic_folder_black);
            // Always set all ViewHolder fields, otherwise recycled views contain wrong data
            holder.mTextViewFolderDetailsLeft.setText("");
            holder.mTextViewFolderDetailsRight.setText("");
            holder.mTextViewFolderDetailsMiddle.setVisibility(View.GONE);

            holder.mProgressBar.setVisibility(View.INVISIBLE);
        } else if (boxObject instanceof BoxExternalFile) {
            BoxExternalFile boxExternal = (BoxExternalFile) boxObject;
            holder.mImageView.setImageResource(drawable.ic_insert_drive_file_black);
            if (boxExternal.getOwner().equals(currentIdentity.getEcPublicKey())) {
                holder.mTextViewFolderDetailsLeft.setText(string.filebrowser_file_is_shared_to_other);
            } else {
                String owner = getOwner(boxExternal);
                holder.mTextViewFolderDetailsLeft.setText(context.getString(string.filebrowser_file_is_shared_from).replace("%1", owner));
            }
            holder.mTextViewFolderDetailsRight.setVisibility(View.GONE);
            holder.mTextViewFolderDetailsMiddle.setVisibility(View.VISIBLE);
            holder.mProgressBar.setVisibility(View.INVISIBLE);
        } else if (boxObject instanceof BoxFile) {
            BoxFile boxFile = (BoxFile) boxObject;
            holder.mTextViewFolderDetailsLeft.setText(formatModificationTime(boxFile));
            holder.mTextViewFolderDetailsRight.setText(FileUtils.byteCountToDisplaySize(boxFile.size));
            if (boxFile.isShared()) {
                holder.mTextViewFolderDetailsMiddle.setText(string.filebrowser_file_is_shared_to_other);
                holder.mTextViewFolderDetailsMiddle.setVisibility(View.VISIBLE);
            } else {
                holder.mTextViewFolderDetailsMiddle.setVisibility(View.GONE);
            }
            holder.mImageView.setImageResource(drawable.ic_insert_drive_file_black);
            holder.mProgressBar.setVisibility(View.INVISIBLE);
        } else if (boxObject instanceof BoxUploadingFile) {
            holder.mTextViewFolderDetailsLeft.setText(string.uploading);
            holder.mTextViewFolderDetailsRight.setText("");
            holder.mTextViewFolderDetailsMiddle.setText("");
            holder.mImageView.setImageResource(drawable.ic_cloud_upload_black_24dp);

            holder.mProgressBar.setVisibility(View.VISIBLE);
        }
        holder.mImageView.setAlpha(0.8f);
    }

    /**
     * return file owner name or key if not in contact list
     */
    private String getOwner(BoxExternalFile boxExternal) {

        Contact contact = mService.getContacts().getByKeyIdentifier(boxExternal.getOwner().getReadableKeyIdentifier());
        if (contact != null) {
            return contact.getAlias();
        }
        return boxExternal.owner.getReadableKeyIdentifier().substring(0, 6);
    }

    private String formatModificationTime(BoxFile boxFile) {

        return dateFormat.format(new Date(boxFile.mtime * 1000));
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

    private boolean loaded;

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

    private final AdapterDataObserver observer = new AdapterDataObserver() {
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
