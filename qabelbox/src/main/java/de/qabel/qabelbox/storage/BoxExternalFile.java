package de.qabel.qabelbox.storage;

public class BoxExternalFile extends BoxFile {

	public String owner;

	public BoxExternalFile(String owner, String block, String name, Long size, Long mtime, byte[] key) {
		super(block, name, size, mtime, key);
		this.owner = owner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		BoxExternalFile that = (BoxExternalFile) o;

		return !(owner != null ? !owner.equals(that.owner) : that.owner != null);

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (owner != null ? owner.hashCode() : 0);
		return result;
	}
}
