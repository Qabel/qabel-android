package de.qabel.qabelbox.storage;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public class BoxFile extends BoxObject implements Parcelable {
	public String block;
	public Long size;
	public Long mtime;
	public byte[] key;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BoxFile boxFile = (BoxFile) o;

		if (block != null ? !block.equals(boxFile.block) : boxFile.block != null) return false;
		if (name != null ? !name.equals(boxFile.name) : boxFile.name != null) return false;
		if (size != null ? !size.equals(boxFile.size) : boxFile.size != null) return false;
		if (mtime != null ? !mtime.equals(boxFile.mtime) : boxFile.mtime != null) return false;
		return Arrays.equals(key, boxFile.key);

	}

	@Override
	public int hashCode() {
		int result = block != null ? block.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (size != null ? size.hashCode() : 0);
		result = 31 * result + (mtime != null ? mtime.hashCode() : 0);
		result = 31 * result + (key != null ? Arrays.hashCode(key) : 0);
		return result;
	}

	public BoxFile(String block, String name, Long size, Long mtime, byte[] key) {
		super(name);
		this.block = block;
		this.size = size;
		this.mtime = mtime;
		this.key = key;
	}

	@Override
	protected BoxFile clone() throws CloneNotSupportedException {
		return new BoxFile(block,name,size,mtime,key);
	}

	protected BoxFile(Parcel in) {
		super(in.readString());
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
