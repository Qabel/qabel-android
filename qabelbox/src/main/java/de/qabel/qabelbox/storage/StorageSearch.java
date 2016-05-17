package de.qabel.qabelbox.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.qabel.qabelbox.exceptions.QblStorageException;

/**
 * Represents a search across a BoxVolume path.
 */
public class StorageSearch {

    private static final String CONTAINS_REGEX = ".*%s.*";
    private static final String CONTAINS_IGNORE_CASE_REGEX = "(?i:.*%s.*)";

    private String path;
    private List<BoxObject> nodeList;
    private List<BoxObject> results;
    private Map<String, BoxObject> pathMapping = new Hashtable<>();

    /**
     * Inits the results with all available files and directories.
     *
     * @param navigation The path from which on the search should begin
     * @throws QblStorageException
     */
    public StorageSearch(BoxNavigation navigation) throws QblStorageException {
        this.path = navigation.getPath();
        setupData(navigation);
    }

    private StorageSearch(String path, List<BoxObject> nodes, List<BoxObject> results, Map<String, BoxObject> pathMapping) {
        this.path = path;
        this.nodeList = nodes;
        this.results = results;
        this.pathMapping = pathMapping;
    }

    private void setupData(BoxNavigation navigation) throws QblStorageException {
        if(!pathMapping.isEmpty()){
            pathMapping.clear();
        }
        List<BoxObject> subNodes = collectAll(navigation);
        this.nodeList = subNodes;
        this.results = new ArrayList<>(subNodes);
    }

    public void refreshRange(BoxNavigation navigation, boolean force) throws QblStorageException {
        if (force || !this.path.equals(navigation.getPath())) {
            setupData(navigation);
            this.path = navigation.getPath();
        }else {
            reset();
        }
    }

    public void refreshRange(BoxNavigation navigation) throws QblStorageException {
        refreshRange(navigation, false);
    }

    public void reset() throws QblStorageException {
        this.results = new ArrayList<>(this.nodeList);
    }

    public boolean isValidSearchTerm(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public List<BoxFile> toBoxFiles(List<BoxObject> lst) {
        List<BoxFile> ret = new ArrayList<>();

        for (BoxObject o : lst) {
            if (o instanceof BoxFile) {
                ret.add((BoxFile) o);
            }
        }

        return ret;
    }

    public List<BoxFolder> toBoxFolders(List<BoxObject> lst) {
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

    public int getResultSize() {
        return this.results.size();
    }

    public String getPath() {
        return this.path;
    }

    /**
     * Table to provide lookup for paths possible.
     *
     * @return A Hashtable which provides the absolute path for a given BoxObject.
     */
    public Map<String, BoxObject> getPathMapping() {
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

        List<BoxObject> filtered = new ArrayList<>();

        String expression = String.format(caseSensitive ? CONTAINS_REGEX : CONTAINS_IGNORE_CASE_REGEX, name);
        for (BoxObject o : results) {
            if (o.name.matches(expression)) {
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
                Date boxDate = new Date(((BoxFile) o).mtime * 1000);
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
        filtered.addAll(toBoxFiles(results));
        results = filtered;

        return this;
    }

    public StorageSearch filterOnlyDirectories() {

        List<BoxObject> filtered = new ArrayList<>();
        filtered.addAll(toBoxFolders(results));
        results = filtered;

        return this;
    }

    public String findPathByBoxObject(BoxObject o) {

        if (o == null) {
            return null;
        }

        for (String path : pathMapping.keySet()) {
            BoxObject tgt = pathMapping.get(path);

            if (tgt.equals(o)) {
                return path;
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

    public StorageSearch sortByName(final boolean caseSensitive) {

        Collections.sort(results, (o1, o2) -> {
            String s1 = o1.name;
            String s2 = o2.name;
            return (caseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2));
        });

        return this;
    }

    private List<BoxObject> collectAll(BoxNavigation navigation) throws QblStorageException {
        List<BoxObject> lst = new ArrayList<>();
        addAll(navigation, lst);
        return lst;
    }

    private void addObject(List<BoxObject> list, BoxNavigation navigation, BoxObject boxObject) {
        list.add(boxObject);
        pathMapping.put(navigation.getPath(boxObject), boxObject);
    }

    private void addAll(BoxNavigation navigation, List<BoxObject> lst) throws QblStorageException {
        for (BoxFile file : navigation.listFiles()) {
            addObject(lst, navigation, file);
        }
        for (BoxObject file : navigation.listExternals()) {
            addObject(lst, navigation, file);
        }
        for (BoxFolder folder : navigation.listFolders()) {
            addObject(lst, navigation, folder);

            navigation.navigate(folder);
            addAll(navigation, lst);
            navigation.navigateToParent();
        }
    }

    @Override
    public StorageSearch clone() throws CloneNotSupportedException {
        return new StorageSearch(new String(this.path), new ArrayList<>(nodeList),
                new ArrayList<>(results), new Hashtable<>(pathMapping));
    }

}
