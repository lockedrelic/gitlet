package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** Representation for a single commit containing the necessary
 * metadata and pointers. Should be immutable.
 * @author Jake Clayton */
public class Commit implements Serializable {

    /** Normal commit constructor. */
    public Commit(String message, HashMap<String, String> contents, String parent) {
        _parent = parent;
        _message = message;
        _timestamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date());
        _hashCode = "c" + Utils.sha1(_timestamp, _message).substring(0, 9);
        _parent2 = null;
        _contents = contents;
    }

    /** Constructor for merge commits. */
    public Commit(String message, HashMap<String, String> contents, String parent, String parent2) {
        _parent = parent;
        _parent2 = parent2;
        _message = message;
        _timestamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date());
        _hashCode = "c" + Utils.sha1(_timestamp, _message).substring(0, 9);
        _contents = contents;
    }

    /** Special constructor for the initial commit. */
    public Commit() {
        _parent = null;
        _parent2 = null;
        _message = "initial commit";
        _timestamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z").format(new Date(0));
        _hashCode = "c" + Utils.sha1(_timestamp, "init").substring(0, 9);
        _contents = new HashMap<String, String>();
    }

    /** Return the SHA-1 ID as a string */
    public final String get_hashCode() {
        return _hashCode;
    }

    /** Prints log information: the SHA-1 ID, time of creation,
     * and the commit message. */
    public final void printLog() {
        System.out.println("===");
        System.out.println("commit " + _hashCode);
        if (_parent2 != null) {
            System.out.println("Merge: " + _parent.substring(0, 7) + " " + _parent2.substring(0, 7));
        }
        System.out.println("Date: " + _timestamp);
        System.out.println(_message);
        System.out.println();
    }

    /** Returns whether the commit contains a pointer to a blob
     * based on its hashcode. */
    public boolean contains(String hashcode) {
        if (_contents == null) {
            return false;
        }
        return _contents.containsValue(hashcode);
    }

    /** Returns whether a commit contains a pointer to a blob
     * based on the corresponding file name. */
    public final boolean containsFile(String fileName) {
        if (_contents == null) {
            return false;
        }
        return _contents.containsKey(fileName);
    }

    /** Returns the hashcode of a blob based on the corresponding file name. */
    public final String getfileHash(String name) {
        return _contents.get(name);
    }

    /** Returns the commit message. */
    public final String getMessage() {
        return _message;
    }

    /** Returns the commit's parent's SHA-1 ID. */
    public final String getParent() {
        return _parent;
    }

    /** Returns the second parent's SHA-1 ID. */
    public final String getParent2() {
        return _parent2;
    }

    /** Returns a commit's contents. */
    public final HashMap<String, String> contents() {
        return _contents;
    }

    /** A date string recorded at time of this commit's creation. */
    private final String _timestamp;

    /** The commit parent's SHA-1 ID. */
    private final String _parent;

    /** Second parent commit in case of a merge.*/
    private final String _parent2;

    /** A commit's SHA-1 ID based on its timestamp and message. */
    private final String _hashCode;

    /** The given commit message. */
    private final String _message;

    /** A HashMap of Blobs saved under this commit. The keys are
     * the corresponding file names and the keys are the SHA-1 ID's.  */
    private final HashMap<String, String> _contents;


}