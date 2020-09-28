package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

// A commit consists of a log message, timestamp, a mapping of file names to blob references, and a single parent reference.
public class Commit implements Serializable {
    /* The commit message (metadata). */
    public String message;

    /* The commit time (metadata). */
    public String timeStamp;

    /* Parent commit of the current commit. */
    public Commit parent;

    /* List of Blob objects that stores the content. */
    public ArrayList<File> files;

    /* Maps commit's file names to sha ids.*/
    public HashMap<String, String> filesMap;

    /* SHA-id of the commit. */
    public String id;

    /* Construct a commit object using known information
     * and generate its as well as its files' SHA-id . */
    public Commit(String message, String timeStamp, Commit parent, HashMap<String, String> filesMap, String id) {
        this.message = message;
        this.timeStamp = timeStamp;
        this.parent = parent;
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

    /* Returns the sha-id of the commit. */
    public String getCommitID() {
        return id;
    }


    /* Checks if the commit contains the file */
    public boolean checkFile(String filename) {
        return filesMap.containsKey(filename);
    }

    /* Returns the sha-id of the file. */
    public String getFileID(String filename) {
        return filesMap.get(filename);
    }

    public void print() {
        System.out.println("Commit " + id);
        System.out.println(timeStamp);
        System.out.println(message);
    }
}
