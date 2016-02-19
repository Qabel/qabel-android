package de.qabel.qabelbox.storage;

import de.qabel.core.crypto.QblECPublicKey;

import java.util.Arrays;

public class BoxExternalReference {
	public boolean isFolder;
	public String name;
	public String prefix;
	public String block;
	public QblECPublicKey owner;
	public byte[] key;

	public BoxExternalReference(boolean isFolder, String prefix, String block, String name, QblECPublicKey owner, byte[] key) {
		this.isFolder = isFolder;
		this.name = name;
		this.prefix = prefix;
		this.block = block;
		this.owner = owner;
		this.key = key;
	}

	public String getRef() {
		return prefix + '/' + block;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BoxExternalReference that = (BoxExternalReference) o;

		if (isFolder != that.isFolder) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
		if (block != null ? !block.equals(that.block) : that.block != null) return false;
		if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
		return Arrays.equals(key, that.key);

	}

	@Override
	public int hashCode() {
		int result = (isFolder ? 1 : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
		result = 31 * result + (block != null ? block.hashCode() : 0);
		result = 31 * result + (owner != null ? owner.hashCode() : 0);
		result = 31 * result + (key != null ? Arrays.hashCode(key) : 0);
		return result;
	}

	@Override
	protected BoxExternalReference clone() throws CloneNotSupportedException {
		return new BoxExternalReference(isFolder, prefix, block, name, owner, key);
	}
}
