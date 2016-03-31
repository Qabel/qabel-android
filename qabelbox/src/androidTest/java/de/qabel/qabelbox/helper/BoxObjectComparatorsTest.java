package de.qabel.qabelbox.helper;

import android.support.annotation.NonNull;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BoxObjectComparatorsTest {

    @Test
    public void testAlphabeticOrderFoldersFirst() {
        List<BoxObject> objects = getShuffledObjects();
        Collections.sort(objects, BoxObjectComparators.alphabeticOrderDirectoriesFirstIgnoreCase());
        verifySortOrder(objects);
    }

    private void verifySortOrder(List<BoxObject> objects) {
        assertThat("B", is(objects.get(0).name));
        assertThat("bb", is(objects.get(1).name));
        assertThat("Folder", is(objects.get(2).name));
        assertThat("folder1", is(objects.get(3).name));
        assertThat("Y", is(objects.get(4).name));
        assertThat("yy", is(objects.get(5).name));
        assertThat("A", is(objects.get(6).name));
        assertThat("aa", is(objects.get(7).name));
        assertThat("File", is(objects.get(8).name));
        assertThat("file1", is(objects.get(9).name));
        assertThat("X", is(objects.get(10).name));
        assertThat("xx", is(objects.get(11).name));
    }

    private List<BoxObject> getShuffledObjects() {
        List<BoxObject> objects = new ArrayList<>();

        objects.add(getBoxFile("A"));
        objects.add(getBoxFile("aa"));
        objects.add(getBoxFile("File"));
        objects.add(getBoxFile("file1"));
        objects.add(getBoxFile("X"));
        objects.add(getBoxFile("xx"));

        objects.add(getBoxFolder("B"));
        objects.add(getBoxFolder("bb"));
        objects.add(getBoxFolder("Folder"));
        objects.add(getBoxFolder("folder1"));
        objects.add(getBoxFolder("Y"));
        objects.add(getBoxFolder("yy"));

        Collections.shuffle(objects);
        return objects;
    }

    @NonNull
    private BoxFolder getBoxFolder(String name) {
        return new BoxFolder("", name, new byte[]{0x01, 0x02});
    }

    @NonNull
    private BoxFile getBoxFile(String name) {
        return new BoxFile("Prefix", "Block", name, 0L, 0L, new byte[]{0x01, 0x02});
    }
}
