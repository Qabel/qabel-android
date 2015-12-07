package de.qabel.qabelbox.storage;

import android.provider.BaseColumns;

public final class FileCacheContract {

	public FileCacheContract() {}

	public static abstract class FileEntry implements BaseColumns {

		public static final String TABLE_NAME = "cache";
		public static final String COL_REF = "ref";
		public static final String COL_PATH = "path";
		public static final String COL_MTIME = "mtime";
		public static final String COL_SIZE = "size";


	}
}
