package de.qabel.qabelbox.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import de.qabel.qabelbox.exceptions.QblStorageException;

/**
 * Represents a search across a BoxVolume path.
 */
public class StorageSearch {

    private BoxNavigation navigation;
    private List<BoxObject> results;
    private Hashtable<String, BoxObject> pathMapping = new Hashtable<>();

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

    public StorageSearch(List<BoxObject> results, Hashtable<String, BoxObject> pathMapping) {
        this.results = results;
        this.pathMapping = pathMapping;
    }

    public static StorageSearch createStorageSearchFromList(List<? extends BoxObject> other) {
        List<BoxObject> lst = new ArrayList<>();

        lst.addAll(other);

        return new StorageSearch(lst);
    }

    public static boolean isValidSearchTerm(String name) {
        return name != null && !"".equals(name.trim());
    }

    public static List<BoxFile> toBoxFiles(List<BoxObject> lst) {
        List<BoxFile> ret = new ArrayList<>();

        for (BoxObject o : lst) {
            if (o instanceof BoxFile) {
                ret.add((BoxFile) o);
            }
        }

        return ret;
    }

    public static List<BoxFolder> toBoxFolders(List<BoxObject> lst) {
        List<BoxFolder> ret = new ArrayList<>();

        for (BoxObject o : lst) {
            if (o instanceof BoxFolder) {
                ret.add((BoxFolder) o);
            }
        }

        return ret;
    }

    /**
     * The flat list of the current resultset.
     *
     * @return The filtered list, all files and dirs if no filter was applied.
     */
    public List<BoxObject> getResults() {
        return results;
    }

    /**
     * Table to provide lookup for paths possible.
     *
     * @return A Hashtable which provides the absolute path for a given BoxObject.
     */
    public Hashtable<String, BoxObject> getPathMapping() {
        return pathMapping;
    }

    public StorageSearch filterByNameCaseSensitive(String name) {
        return filterByName(name, true);
    }

    public StorageSearch filterByNameCaseInsensitive(String name) {
        return filterByName(name, false);
    }

    public StorageSearch filterByName(String name) {
        return filterByNameCaseInsensitive(name);
    }

    public StorageSearch filterByName(String name, boolean caseSensitive) {

        if (!isValidSearchTerm(name)) {
            return this;
        }

        if (!caseSensitive) {
            name = name.toLowerCase();
        }

        List<BoxObject> filtered = new ArrayList<>();

        for (BoxObject o : results) {
            String objKey = caseSensitive ? o.name : o.name.toLowerCase();

            if (objKey.indexOf(name) >= 0) {
                filtered.add(o);
            }
        }

        results = filtered;

        return this;
    }

    public StorageSearch filterByMaximumSize(long size) {
        return filterBySize(size, false);
    }

    public StorageSearch filterByMinimumSize(long size) {
        return filterBySize(size, true);
    }

    public StorageSearch filterBySize(long size, boolean minSize) {

        List<BoxObject> filtered = new ArrayList<>();

        for (BoxObject o : results) {
            if (o instanceof BoxFile) {
                BoxFile f = (BoxFile) o;

                if ((minSize && f.size >= size) || (!minSize && f.size <= size)) {
                    filtered.add(o);
                }
            }
        }

        results = filtered;

        return this;
    }

    public StorageSearch filterByExtension(String extension) {

        if (extension == null) {
            results.clear();

            return this;
        }

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        List<BoxObject> filtered = new ArrayList<>();

        for (BoxObject o : results) {
            if (o instanceof BoxFile) {
                BoxFile f = (BoxFile) o;

                if (f.name.toLowerCase().endsWith(extension.toLowerCase())) {
                    filtered.add(o);
                }
            }
        }

        results = filtered;

        return this;
    }

    public StorageSearch filterByMinimumDate(Date date) {
        return filterByDate(date, true);
    }

    public StorageSearch filterByMaximumDate(Date date) {
        return filterByDate(date, false);
    }

    /**
     * This method will filter by last modified and return only files.
     *
     * @param date
     * @param minDate
     * @return
     */
    public StorageSearch filterByDate(Date date, boolean minDate) {

        List<BoxObject> filtered = new ArrayList<>();

        for (BoxObject o : results) {
            if (o instanceof BoxFile) {
                Date boxDate = new Date(((BoxFile) o).mtime*1000);
                boolean isBeforeMinimumDate = boxDate.before(date) && date.getTime() != boxDate.getTime();
                boolean isAfterMaximumDate = boxDate.after(date);
                boolean isInvalid = (minDate && isBeforeMinimumDate) || (!minDate && isAfterMaximumDate);

                if (!isInvalid) {
                    filtered.add(o);
                }
            }
        }

        results = filtered;

        return this;
    }

    public StorageSearch filterOnlyFiles() {

        List<BoxObject> filtered = new ArrayList<>();
        filtered.addAll(StorageSearch.toBoxFiles(results));
        results = filtered;

        return this;
    }

    public StorageSearch filterOnlyDirectories() {

        List<BoxObject> filtered = new ArrayList<>();
        filtered.addAll(StorageSearch.toBoxFolders(results));
        results = filtered;

        return this;
    }

    public String findPathByBoxObject(BoxObject o) {

        if (o == null) {
            return null;
        }

        for (String path : pathMapping.keySet()) {
            BoxObject tgt = pathMapping.get(path);

            //BoxObject does not implement equals (failed as pathMapping-key), but normal equals might work here?
            if (o instanceof BoxFile && tgt instanceof BoxFile) {
                if (((BoxFile) o).equals((BoxFile) tgt)) {
                    return path;
                }
            } else if (o instanceof BoxFolder && tgt instanceof BoxFolder) {
                if (((BoxFolder) o).equals((BoxFolder) tgt)) {
                    return path;
                }
            }
        }

        return null;
    }

    public BoxObject findByPath(String path) {

        if (path == null) {
            return null;
        }

        return pathMapping.containsKey(path) ? pathMapping.get(path) : null;
    }

    public StorageSearch sortCaseSensitiveByName() {
        return sortByName(true);
    }

    public StorageSearch sortCaseInsensitiveByName() {
        return sortByName(false);
    }

    public StorageSearch sortByName(boolean caseSensitive) {

        if (caseSensitive) {
            Collections.sort(results, new Comparator<BoxObject>() {

                public int compare(BoxObject o1, BoxObject o2) {
                    String s1 = o1.name;
                    String s2 = o2.name;
                    return s1.compareTo(s2);
                }
            });
        } else {
            Collections.sort(results, new Comparator<BoxObject>() {

                public int compare(BoxObject o1, BoxObject o2) {
                    String s1 = o1.name;
                    String s2 = o2.name;
                    return s1.toLowerCase().compareTo(s2.toLowerCase());
                }
            });
        }

        return this;
    }

    private List<BoxObject> collectAll() throws QblStorageException {
        List<BoxObject> lst = new ArrayList<>();

        addAll(lst);

        return lst;
    }

    private void addAll(List<BoxObject> lst) throws QblStorageException {

        for (BoxFile file : navigation.listFiles()) {
            lst.add(file);

            pathMapping.put(navigation.getPath(file), file);
        }

        for (BoxFolder folder : navigation.listFolders()) {
            lst.add(folder);

            pathMapping.put(navigation.getPath(folder), folder);

            navigation.navigate(folder);
            addAll(lst);
            navigation.navigateToParent();
        }
    }

    @Override
    public StorageSearch clone() throws CloneNotSupportedException {
        return new StorageSearch(cloneResultList(getResults()), (Hashtable<String, BoxObject>) getPathMapping().clone());
    }

    private List<BoxObject> cloneResultList(List<BoxObject> list) throws CloneNotSupportedException {
        List<BoxObject> clone = new ArrayList<>(list.size());
        for (BoxObject item : list) {
            if (item instanceof BoxFile) {
                clone.add(((BoxFile) item).clone());
            }
            if (item instanceof BoxExternalFile) {
                clone.add(((BoxExternalFile) item).clone());
            } else if (item instanceof BoxExternalFolder){
				clone.add(((BoxExternalFolder) item).clone());
			} else if (item instanceof BoxFolder) {
                clone.add(((BoxFolder) item).clone());
            }
        }
        return clone;
    }
}
