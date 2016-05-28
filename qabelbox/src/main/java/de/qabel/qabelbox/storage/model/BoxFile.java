package de.qabel.qabelbox.storage.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public class BoxFile extends BoxObject implements Parcelable {
    public String prefix;
    public String block;
    public Long size;
    public Long mtime;
    public byte[] key;
    public String meta;
    public byte[] metakey;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoxFile boxFile = (BoxFile) o;

        if (prefix != null ? !prefix.equals(boxFile.prefix) : boxFile.prefix != null) return false;
        if (block != null ? !block.equals(boxFile.block) : boxFile.block != null) return false;
        if (size != null ? !size.equals(boxFile.size) : boxFile.size != null) return false;
        if (mtime != null ? !mtime.equals(boxFile.mtime) : boxFile.mtime != null) return false;
        if (!Arrays.equals(key, boxFile.key)) return false;
        if (meta != null ? !meta.equals(boxFile.meta) : boxFile.meta != null) return false;
        return Arrays.equals(metakey, boxFile.metakey);

    }

    @Override
    public int hashCode() {
        int result = prefix != null ? prefix.hashCode() : 0;
        result = 31 * result + (block != null ? block.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (mtime != null ? mtime.hashCode() : 0);
        result = 31 * result + (key != null ? Arrays.hashCode(key) : 0);
        result = 31 * result + (meta != null ? meta.hashCode() : 0);
        result = 31 * result + (metakey != null ? Arrays.hashCode(metakey) : 0);
        return result;
    }

    public BoxFile(String prefix, String block, String name, Long size, Long mtime, byte[] key) {
        super(name);
        this.prefix = prefix;
        this.block = block;
        this.size = size;
        this.mtime = mtime;
        this.key = key;
    }

    public BoxFile(String prefix, String block, String name, Long size, Long mtime, byte[] key, String meta, byte[] metaKey) {
        super(name);
        this.prefix = prefix;
        this.block = block;
        this.size = size;
        this.mtime = mtime;
        this.meta = meta;
        this.metakey = metaKey;
        this.key = key;
    }

    @Override
    protected BoxFile clone() throws CloneNotSupportedException {
        return new BoxFile(prefix, block, name, size, mtime, key, meta, metakey);
    }

    /**
     * Get if BoxFile is shared. Tests only if meta and metakey is not null, not if a share has been
     * successfully send to another user.
     *
     * @return True if BoxFile might be shared.
     */
    public boolean isShared() {
        if (meta != null && metakey != null) {
            return true;
        }
        return false;
    }

    protected BoxFile(Parcel in) {
        super(in.readString());
        prefix = in.readString();
        block = in.readString();
        key = new byte[in.readInt()];
        in.readByteArray(key);
        size = in.readByte() == 0x00 ? null : in.readLong();
        mtime = in.readByte() == 0x00 ? null : in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(prefix);
        dest.writeString(block);
        dest.writeByteArray(key);
        if (size == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(size);
        }
        if (mtime == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(mtime);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<BoxFile> CREATOR = new Parcelable.Creator<BoxFile>() {
        @Override
        public BoxFile createFromParcel(Parcel in) {
            return new BoxFile(in);
        }

        @Override
        public BoxFile[] newArray(int size) {
            return new BoxFile[size];
        }
    };

}
