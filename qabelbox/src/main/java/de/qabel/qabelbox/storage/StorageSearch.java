package de.qabel.qabelbox.storage;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.qabel.qabelbox.exceptions.QblStorageException;

/**
 * Represents a search across a BoxVolume path.
 */
public class StorageSearch {

    private BoxNavigation navigation;
    private List<BoxObject> results;

    /**
     * Inits the results with all available files and directories.
     *
     * @param navigation The path from which on the search should begin
     * @throws QblStorageException
     */
    public StorageSearch(BoxNavigation navigation) throws QblStorageException {
        this.navigation = navigation;
        results = collectAll();
    }

    /**
     * Construct a search using a resultset and don't hit the storage volume.
     *
     * @param results
     */
    public StorageSearch(List<BoxObject> results) {
        this.results = results;
    }

    /**
     * The flat list of the current resultset.
     *
     * @return The filtered list, all files and dirs if no filter was applied.
     */
    public List<BoxObject> getResults() {
        return results;
    }


    public static boolean isValidSearchTerm(String name) {
        return name != null && !"".equals(name.trim());
    }


    public StorageSearch filterByNameCaseSensitive(String name) {
        filterByName(name, true);
        return this;
    }

    public StorageSearch filterByNameCaseInsensitive(String name) {
        filterByName(name, false);
        return this;
    }
    public StorageSearch filterByName(String name) {
        filterByNameCaseInsensitive(name);
        return this;
    }

    public StorageSearch filterByName(String name, boolean caseSensitive) {

        if(!isValidSearchTerm(name)) {
            return this;
        }

        if(!caseSensitive) {
            name = name.toLowerCase();
        }

        List<BoxObject> filtered = new ArrayList<>();

        for(BoxObject o : results) {
            String objKey = caseSensitive ? o.name : o.name.toLowerCase();

            if(objKey.indexOf(name) >= 0) {
                filtered.add(o);
            }
        }

        results = filtered;

        return this;
    }

    private List<BoxObject> collectAll() throws QblStorageException {
        List<BoxObject> lst = new ArrayList<>();

        addAll(lst);

        return lst;
    }

    private void addAll(List<BoxObject> lst) throws QblStorageException {

        for(BoxFile file : navigation.listFiles()) {
            lst.add(file);
        }

        for(BoxFolder folder : navigation.listFolders()) {
            lst.add(folder);

            navigation.navigate(folder);
            addAll(lst);
            navigation.navigateToParent();
        }
    }

}
