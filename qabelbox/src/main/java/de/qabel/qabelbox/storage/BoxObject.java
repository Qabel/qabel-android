package de.qabel.qabelbox.storage;

public class BoxObject implements Comparable<BoxObject>  {
	public String name;

	public BoxObject(String name) {this.name = name;}

	@Override
	public int compareTo(BoxObject another) {
		if (this instanceof BoxFile && another instanceof BoxFile) {
			return this.name.compareTo(another.name);
		}
		if (this instanceof BoxFolder && (another instanceof BoxFolder || another instanceof BoxExternal)) {
			return this.name.compareTo(another.name);
		}
		if (this instanceof BoxExternal && (another instanceof BoxFolder || another instanceof BoxExternal)) {
			return this.name.compareTo(another.name);
		}
		if (this instanceof BoxFolder || this instanceof BoxExternal) {
			return -1;
		}
		return 1;
	}
}
