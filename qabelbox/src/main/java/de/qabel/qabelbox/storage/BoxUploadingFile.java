package de.qabel.qabelbox.storage;

import de.qabel.qabelbox.R;

public class BoxUploadingFile extends BoxObject {

	public long totalSize;
	public long uploadedSize;

	public BoxUploadingFile(String name) {
		super(name);
	}

	public int getUploadStatusPercent() {
		if (totalSize == 0 || uploadedSize == 0) {
			return 0;
		}
		return (int) (100 * uploadedSize / totalSize);
	}

	@Override
	public int getBottomSheetMenuResId() {
		return R.menu.box_uploading_file_bottom_sheet;
	}
}
