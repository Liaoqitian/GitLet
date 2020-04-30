package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.io.Serializable;

public class Commit implements Serializable {
    /* The commit message (metadata). */
    public String message;

    /* The commit time (metadata). */
    public String timeStamp;

    /* Parent commit of the current commit. */
    public Commit parent;

    /* Siblings' id of the commit object. */
    public ArrayList<String> sibling;

    /* List of Blob objects that stores the content. */
    public ArrayList<File> files;

    /* A map whose key is the filename, and the value is it corresponding sha id.*/
    public Map<String, String> filesMap;

    /* SHA-id of the commit. */
    public String id;

    /* Construct a commit object using known information
     * and generate its as well as its files' SHA-id . */
    public Commit(String message, String timeStamp, Commit parent,
                  ArrayList<String> sibling, ArrayList<File> files, Map filesMap, String id) {
        this.message = message;
        this.timeStamp = timeStamp;
        this.parent = parent;
        this.sibling = sibling;
        this.files = files;
        this.filesMap = filesMap;
        this.id = id;
    }

    /* Returns the commit message. */
    public String getMessage() {
        return message;
    }

    /* Returns the timestamp. */
    public String getTimeStamp() {

        return timeStamp;
    }

    /* Returns the parent's id. */
    public Commit getParent() {

        return parent;
    }

    /* Returns the list of files. */
    public ArrayList<File> getFile() {

        return files;
    }

    /* Returns the commit id. */
    public String getID() {

        return id;
    }

    /* Returns the commit id. */
    public String latestID(String filename) {

        return filesMap.get(filename);
    }

    public void print() {
        System.out.println("Commit " + id);
        System.out.println(timeStamp);
        System.out.println(message);
    }
}
