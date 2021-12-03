package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Representation of the staging area.
 * @author Jake Clayton */
public class StagingArea {

    /** The command working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory where all persistence is stored. */
    static final File GITLET_FOLDER = Main.getDirectory();

    /** Subdictory where all staging area files are persisted. */
    static final File STAGING_FOLDER = Utils.join(GITLET_FOLDER, "staging");

    /** Subdirectory of staging where files are staged to be added. */
    static final File ADDITION_FOLDER = Utils.join(STAGING_FOLDER, "addition");

    /** Subdirectory of staging where files are staged to be removed. */
    static final File REMOVAL_FOLDER = Utils.join(STAGING_FOLDER, "removal");

    /** This is a file where branch and branch head information is stored. */
    static final File INFO_FILE = Utils.join(GITLET_FOLDER, "info");

    public static void initialize() {
        assert GITLET_FOLDER.exists();
        if (!STAGING_FOLDER.exists()) {
            STAGING_FOLDER.mkdir();
        }
        if (!ADDITION_FOLDER.exists()) {
            ADDITION_FOLDER.mkdir();
        }
        if (!REMOVAL_FOLDER.exists()) {
            REMOVAL_FOLDER.mkdir();
        }
    }

    /** Stages a file to be added. */
    public static void add(File file) {
        if (!file.exists()) {
            File d = Utils.join(REMOVAL_FOLDER, file.getName());
            if (!d.exists()) {
                Main.error("File does not exist.");
            }
            Blob reAdd = Utils.readObject(d, Blob.class);
            d.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(file, reAdd.returnContents());
            return;
        }
        Commit head = CommitTree.getHead();
        Blob newBlob = new Blob(file);
        if (head.contains(newBlob.get_hashCode())) {
            File check = Utils.join(ADDITION_FOLDER, file.getName());
            if (check.exists()) {
                check.delete();
            }
            check = Utils.join(REMOVAL_FOLDER, file.getName());
            if (check.exists()) {
                check.delete();
            }
        } else {
            File check = Utils.join(REMOVAL_FOLDER, file.getName());
            if (check.exists()) {
                check.delete();
            }
            File stageThis = Utils.join(ADDITION_FOLDER, file.getName());
            if (stageThis.exists()) {
                stageThis.delete();
            }
            try {
                stageThis.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(stageThis, newBlob);
        }
    }

    /** Returns whether there are no files staged to be added or removed. */
    public static boolean isEmpty() {
        if (ADDITION_FOLDER.listFiles().length > 0 || REMOVAL_FOLDER.listFiles().length > 0) {
            return false;
        }
        return true;
    }

    /** Returns an array of files staged to be added. */
    public static File[] filestoAdd() {
        return ADDITION_FOLDER.listFiles();
    }

    /** Returns an array of files staged to be removed. */
    public static File[] filestoRemove() {
        return REMOVAL_FOLDER.listFiles();
    }

    /** Deletes all staged files. */
    public static void clear() {
        if (isEmpty()) {
            return;
        }
        File[] add = ADDITION_FOLDER.listFiles();
        for (File file : add) {
            file.delete();
        }
        File[] remove = REMOVAL_FOLDER.listFiles();
        for (File file : remove) {
            file.delete();
        }
    }

    /** Removes a given file from the CWD if it exists and stages it to be removed.
     * It is no longer staged to be added if it was at the time of execution.
     * @param fileName */
    public static void rm(String fileName) {
        File check = Utils.join(ADDITION_FOLDER, fileName);
        Commit head = CommitTree.getHead();
        boolean removed = false;
        Blob toDelete = null;
        if (check.exists()) {
            toDelete = new Blob(check);
            check.delete();
            removed = true;
        }
        if (head.containsFile(fileName)) {
            check = Utils.join(CWD, fileName);
            if (check.exists()) {
                toDelete = new Blob(check);
                check.delete();
            } else if (check == null) {
                Main.error("No reason to remove the file.");
            }
            File delete = Utils.join(REMOVAL_FOLDER, fileName);
            if (!delete.exists()) {
                try {
                    delete.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File f = Utils.join(REMOVAL_FOLDER, fileName);
            Utils.writeObject(f, toDelete);
        } else if (!removed){
            Main.error("No reason to remove the file.");
        }
    }

    /** Prints out required information for the status command.
     * Returns branch information, staged files, removed files,
     * untracked files, and modifications not staged for commit.*/
    @SuppressWarnings("unchecked")
    public static void printStatus() {
        HashMap<String, String> TrackingInfo = Utils.readObject(INFO_FILE, HashMap.class);
        System.out.println("=== Branches ===");
        System.out.println("*" + TrackingInfo.get("Active Branch"));
        for (String entry : TrackingInfo.keySet()) {
            if (!entry.equals("Active Branch") && !entry.equals("Active Head")) {
                System.out.println(entry);
            }
        }
        System.out.println();
        HashSet<String> mainFiles = new HashSet<>(Utils.plainFilenamesIn(CWD));
        Commit head = CommitTree.getHead();
        System.out.println("=== Staged Files ===");
        List<String> staged = Utils.plainFilenamesIn(ADDITION_FOLDER);
        for (String fileName : staged) {
            System.out.println(fileName);
        }
        System.out.println();
        HashSet<String> stagedSet = new HashSet<>(staged);
        System.out.println("=== Removed Files ===");
        List<String> removed = Utils.plainFilenamesIn(REMOVAL_FOLDER);
        for (String fileName : removed) {
            System.out.println(fileName);
        }
        System.out.println();
        HashSet<String> removedSet = new HashSet<>(removed);
        System.out.println("=== Modifications Not Staged For Commit ===");
        HashMap<String, String> trackedFiles = head.contents();
        ArrayList<String> untracked = new ArrayList<>();
        HashSet<String> checkFiles = new HashSet<>(staged);
        checkFiles.addAll(mainFiles);
        checkFiles.addAll(trackedFiles.keySet());
        for (String fileName : checkFiles) {
            if (!trackedFiles.containsKey(fileName) && mainFiles.contains(fileName) && !stagedSet.contains(fileName) && !removedSet.contains(fileName)) {
                untracked.add(fileName);
            }
            if (stagedSet.contains(fileName)) {
                if (!mainFiles.contains(fileName)) {
                    System.out.println(fileName + " (deleted)");
                } else if (!new Blob(Utils.join(CWD, fileName)).get_hashCode().equals(Utils.readObject(Utils.join(ADDITION_FOLDER, fileName), Blob.class).get_hashCode())) {
                    System.out.println(fileName + " (modified)");
                }
            } else if (trackedFiles.containsKey(fileName)) {
                if (!mainFiles.contains(fileName)) {
                    if (!removedSet.contains(fileName)) {
                        System.out.println(fileName + " (deleted)");
                    }
                } else {
                    String mainContents = new Blob(Utils.join(CWD, fileName)).get_hashCode();
                    File f = Utils.join(GITLET_FOLDER, head.contents().get(fileName));
                    String commitContents = Utils.readObject(f, Blob.class).get_hashCode();
                    if (!mainContents.equals(commitContents)) {
                        System.out.println(fileName + " (modified)");
                    }
                }
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String fileName : untracked) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    /** Returns whether a file is staged to be added or not. */
    public static boolean isStaged(String fileName) {
        File check = Utils.join(ADDITION_FOLDER, fileName);
        return check.exists();
    }
}