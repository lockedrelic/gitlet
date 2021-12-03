package gitlet;

import java.io.File;
import java.io.Serializable;

/** Representation of a blob, where file contents are serialized in addition
 * to other metadata.
 * @author Jake Clayton */
public class Blob implements Serializable {

    /** Blob constructor. Should be immutable once created. */
    public Blob(File file) {
        _name = file.getName();
        _contents = Utils.readContents(file);
        _stringcontents = Utils.readContentsAsString(file);
        _hashCode = "b" + Utils.sha1(_contents, _name).substring(0, 9);
    }

    /** Return the SHA-1 ID of the blob. */
    public final String get_hashCode() {
        return _hashCode;
    }

    /** Return the byte array contents of a blob. */
    public final byte[] returnContents() {
        return _contents;
    }

    /** Return a string representation of the contents of a blob. */
    public final String getcontentsasString() {
        return _stringcontents;
    }

    /** The SHA-1 ID, based on a blob's name and contents. */
    private final String _hashCode;

    /** The corresponding file name. */
    private transient final String _name;

    /** The corresponding file contents stored in a byte array. */
    private final byte[] _contents;

    /** The corresponding file contents stored as a string. */
    private final String _stringcontents;

}