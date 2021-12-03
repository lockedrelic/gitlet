package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/** Represents the commit tree, where commits are made and serialized. */
public class CommitTree {

    /** The command working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory where all persistence is stored. */
    static final File GITLET_FOLDER = Main.getDirectory();

    /** This is a file where all commits and their ID's are stored. */
    static final File COMMIT_HISTORY = Utils.join(GITLET_FOLDER, "commit_history");

    /** This is a file where branch and branch head information is stored. */
    static final File INFO_FILE = Utils.join(GITLET_FOLDER, "info");

    /** Simple helper function for updating _commithistory and _info from stored states. */
    @SuppressWarnings("unchecked")
    private static void update() {
        _info = Utils.readObject(INFO_FILE, HashMap.class);
        _commithistory = Utils.readObject(COMMIT_HISTORY, HashMap.class);
    }

    /** Saves changes made to _info and _commithistory into persistence. */
    private static void save() {
        Utils.writeObject(INFO_FILE, _info);
        Utils.writeObject(COMMIT_HISTORY, _commithistory);
    }

    /** Adds a commit, updates persistence files, and clears the staging area. */
    @SuppressWarnings("unchecked")
    public static void addCommit(String message, String secondparent) {
        if (StagingArea.isEmpty()) {
            Main.error("No changes added to the commit");
        }
        update();
        Commit lastCommit = _commithistory.get(_info.get("Active Head"));
        HashMap<String, String> oldContents = lastCommit.contents();
        HashMap<String, String> newContents = new HashMap<>(oldContents);
        File[] toCommit = StagingArea.filestoAdd();
        File[] toRemove = StagingArea.filestoRemove();
        Blob cur;
        for (int i = 0; i < toCommit.length; i++) {
            File process = toCommit[i];
            String name = process.getName();
            cur = Utils.readObject(process, Blob.class);
            if (oldContents.containsKey(name) && oldContents.get(name).equals(cur.get_hashCode())) {
            } else {
                newContents.put(name, cur.get_hashCode());
                File newSave = new File(GITLET_FOLDER, cur.get_hashCode());
                if (!newSave.exists()) {
                    try {
                        newSave.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Utils.writeObject(newSave, cur);
            }
            process.delete();
        }
        for (int i = 0; i < toRemove.length; i++) {
            File process = toRemove[i];
            newContents.remove(process.getName());
            process.delete();
        }
        Commit newHead;
        if (secondparent == null) {
            newHead = new Commit(message, newContents, lastCommit.get_hashCode());
        } else {
            newHead = new Commit(message, newContents, lastCommit.get_hashCode(), secondparent);
        }
        _info.replace("Active Head", newHead.get_hashCode());
        _commithistory.put(newHead.get_hashCode(), newHead);
        save();
    }

    /** Creates the first commit, initializes appropriate persistence files. */
    @SuppressWarnings("unchecked")
    public static void initCommit() {
        Commit firstCommit = new Commit();
        _info = new HashMap<String, String>();
        _commithistory = new HashMap<String, Commit>();
        _info.put("Active Branch", "master");
        _info.put("Active Head", firstCommit.get_hashCode());
        _commithistory.put(firstCommit.get_hashCode(), firstCommit);
        save();
    }

    /** Retrieves the current head commit. */
    public static Commit getHead() {
        update();
        return _commithistory.get(_info.get("Active Head"));
    }

    /** Retrieves a commit based on its SHA-1 ID. */
    public static Commit getCommit(String ID) {
        update();
        if (!_commithistory.containsKey(ID)) {
            Main.error("No commit with that id exists.");
        }
        return _commithistory.get(ID);
    }

    /** Prints out the commit log information in the head commit's history. */
    public static void printCommitHistory() {
        Commit temp = getHead();
        while (true) {
            temp.printLog();
            if (temp.getParent() == null) {
                return;
            }
            temp = _commithistory.get(temp.getParent());
        }
    }

    /** Prints out the commit log information for every commit made. */
    public static void printAllCommits() {
        update();
        for (Commit commit : _commithistory.values()) {
            commit.printLog();
        }
    }

    /** Logic and work for the git checkout command. */
    @SuppressWarnings("unchecked")
    public static void checkout(String... args) {
        Commit head = getHead();
        if (args.length == 1) {
            if (_info.get("Active Branch").equals(args[0])) {
                Main.error("No need to checkout the current branch.");
            } else if (!_info.containsKey(args[0])) {
                Main.error("No such branch exists.");
            }
            Commit newHead = _commithistory.get(_info.get(args[0]));
            for (String fileName : newHead.contents().keySet()) {
                if (!head.containsFile(fileName) && Utils.join(CWD, fileName).exists()) {
                    Main.error("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            }
            HashMap<String, String> overwrite = new HashMap<>(newHead.contents());
            HashMap<String, String> curToDelete = new HashMap<>(head.contents());
            for (String fileName : overwrite.keySet()) {
                curToDelete.remove(fileName);
                File old = Utils.join(CWD, fileName);
                if (!old.exists()) {
                    try {
                        old.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                File b = Utils.join(GITLET_FOLDER, overwrite.get(fileName));
                Utils.writeContents(old, Utils.readObject(b, Blob.class).returnContents());
            }
            for (String name : curToDelete.keySet()) {
                Utils.join(CWD, name).delete();
            }
            _info.remove(args[0]);
            _info.put(_info.get("Active Branch"), _info.get("Active Head"));
            _info.replace("Active Branch", args[0]);
            _info.replace("Active Head", newHead.get_hashCode());
            StagingArea.clear();
            save();
        } else if (args[0].equals("--") && args.length == 2) {
            if (!head.containsFile(args[1])) {
                Main.error("File does not exist in that commit.");
            }
            File thing = Utils.join(GITLET_FOLDER, head.getfileHash(args[1]));
            assert thing.exists();
            File changeThis = Utils.join(CWD, args[1]);
            if (!changeThis.exists()) {
                try {
                    changeThis.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Blob changeTo = Utils.readObject(thing, Blob.class);
            Utils.writeContents(changeThis, changeTo.returnContents());
        } else if (args.length == 3 && args[1].equals("--")) {
            update();
            if (!_commithistory.containsKey(args[0])) {
                Main.error("No commit with that id exists.");
            }
            Commit ref = _commithistory.get(args[0]);
            if (!ref.containsFile(args[2])) {
                Main.error("File does not exist in that commit.");
            }
            File thing = Utils.join(GITLET_FOLDER, ref.getfileHash(args[2]));
            assert thing.exists();
            File changeThis = Utils.join(CWD, args[2]);
            if (!changeThis.exists()) {
                try {
                    changeThis.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Blob changeTo = Utils.readObject(thing, Blob.class);
            Utils.writeContents(changeThis, changeTo.returnContents());
        } else {
            Main.error("Incorrect operands.");
        }
    }

    /** Goes through all commits and prints out their ID's if they contain
     * the given message.
     * @param args */
    public static void find(String... args) {
        if (args.length != 2) {
            Main.error("Incorrect operands.");
        }
        update();
        boolean found = false;
        for (Commit commit : _commithistory.values()) {
            if (commit.getMessage().equals(args[1])) {
                System.out.println(commit.get_hashCode());
                found = true;
            }
        }
        if (!found) {
            Main.error("Found no commit with that message.");
        }
    }

    /** Creates a new branch pointer at the current commit. */
    public static void branch(String branch) {
        Commit head = getHead();
        if (_info.containsKey(branch)) {
            Main.error("A branch with that name already exists.");
        }
        _info.put(branch, head.get_hashCode());
        save();
    }

    /** Removes the branch pointer but does not delete any commits or files. */
    public static void rm_branch(String branch) {
        update();
        if (_info.get("Active Branch").equals(branch)) {
            Main.error("Cannot remove the current branch.");
        } else if (!_info.containsKey(branch)) {
            Main.error("A branch with that name does not exist.");
        }
        _info.remove(branch);
        save();
    }

    /** Resets the CWD to the contents of a commit given its ID.
     * Clears the staging area, the given commit is now the active head. */
    public static void reset(String... args) {
        if (args.length != 2) {
            Main.error("Incorrect operands.");
        }
        HashMap<String, String> tracked = getHead().contents();
        if (!_commithistory.containsKey(args[1])) {
            Main.error("No commit with that id exists.");
        }
        Commit newHead = _commithistory.get(args[1]);
        HashMap<String, String> newCont = newHead.contents();
        HashMap<String, String> check = new HashMap<>(newCont);
        check.putAll(tracked);
        for (String fileName : newCont.keySet()) {
            if (!tracked.containsKey(fileName) && Utils.join(CWD, fileName).exists()) {
                Main.error("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
        for (String fileName : check.keySet()) {
            if (newCont.containsKey(fileName)) {
                checkout(newHead.get_hashCode(), "--", fileName);
            }
            if (tracked.containsKey(fileName) && !newCont.containsKey(fileName)) {
                File remove = Utils.join(CWD, fileName);
                if (remove.exists()) {
                    remove.delete();
                }
            }
        }
        _info.replace("Active Head", newHead.get_hashCode());
        StagingArea.clear();
        save();
    }

    /** Helper function for merge to find the splitpoint commit. */
    private static Commit findSplit(HashSet ids, HashSet<Commit> commits) {
        String ID;
        HashSet copy = new HashSet<>();
        for (Commit check : commits) {
            ID = check.get_hashCode();
            if (ids.contains(ID)) {
                return _commithistory.get(ID);
            }
            if (check.getParent2() != null) {
                copy.add(_commithistory.get(check.getParent2()));
            }
            copy.add(_commithistory.get(check.getParent()));
        }
        return findSplit(ids, copy);
    }

    /** Carries out most of the logic and work for the merge command. */
    public static void merge(String... args) {
        if (args.length != 2) {
            Main.error("Incorrect operands.");
        } else if (!StagingArea.isEmpty()) {
            Main.error("You have uncommitted changes.");
        }
        Commit h = getHead();
        if (_info.get("Active Branch").equals(args[1])) {
            Main.error("Cannot merge a branch with itself.");
        } else if (!_info.containsKey(args[1])) {
            Main.error("A branch with that name does not exist.");
        }
        Commit m = _commithistory.get(_info.get(args[1]));
        HashSet<String> parentSet = new HashSet<>();
        parentSet.add(m .get_hashCode());
        Commit temp = m;
        while (true) {
            if (temp.getParent() == null) {
                break;
            }
            parentSet.add(temp.getParent());
            if (temp.getParent2() != null) {
                parentSet.add(temp.getParent2());
            }
            temp = _commithistory.get(temp.getParent());
        }
        HashSet<Commit> checkSet = new HashSet<>();
        checkSet.add(h);
        Commit splitPoint = findSplit(parentSet, checkSet);

        if (splitPoint.get_hashCode().equals(_info.get("Active Head"))) {
            checkout(args[1]);
            System.out.println("Current branch fast-forwarded.");
            return;
        } else if (splitPoint.get_hashCode().equals(_info.get(args[1]))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        HashMap<String, String> S = splitPoint.contents();
        HashMap<String, String> H = h.contents();
        HashMap<String, String> B = m .contents();
        HashMap<String, String> allContents = new HashMap<>();
        allContents.putAll(S);
        allContents.putAll(H);
        allContents.putAll(B);
        for (String fileName : B.keySet()) {
            if (!h.containsFile(fileName) && Utils.join(CWD, fileName).exists()) {
                Main.error("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
        for (String f : allContents.keySet()) {
            boolean conflict = false;
            if (S.containsKey(f) && H.containsKey(f) && B.containsKey(f)) {
                if (S.get(f).equals(H.get(f)) && !S.get(f).equals(B.get(f))) {
                    checkout(m .get_hashCode(), "--", f);
                    StagingArea.add(Utils.join(CWD, f));
                } else if (S.get(f).equals(B.get(f)) && !S.get(f).equals(H.get(f))) {
                } else if (H.get(f).equals(B.get(f)) && !S.get(f).equals(H.get(f))) {
                } else if (!S.get(f).equals(B.get(f)) && !S.get(f).equals(H.get(f))) {
                    if (!B.get(f).equals(H.get(f))) {
                        conflict = true;
                    }
                }
            } else if (!S.containsKey(f) && H.containsKey(f) && !B.containsKey(f)) {
            } else if (!S.containsKey(f) && !H.containsKey(f) && B.containsKey(f)) {
                checkout(m.get_hashCode(), "--", f);
                StagingArea.add(Utils.join(CWD, f));
            } else if (S.containsKey(f) && S.get(f).equals(H.get(f)) && isAbsent(f, m)) {
                StagingArea.rm(f);
            } else if (S.containsKey(f) && S.get(f).equals(B.get(f)) && isAbsent(f, h)) {
            } else if (S.containsKey(f) && !B.containsKey(f) && !H.containsKey(f)) {
            } else {
                if (!H.containsKey(f) && !S.get(f).equals(B.get(f))) {
                    conflict = true;
                } else if (!B.containsKey(f) && !S.get(f).equals(H.get(f))) {
                    conflict = true;
                } else if (!S.containsKey(f) && !H.get(f).equals(B.get(f))) {
                    conflict = true;
                }
            }
            if (conflict) {
                Blob cur = null;
                Blob merge = null;
                if (H.containsKey(f)) {
                    cur = Utils.readObject(Utils.join(GITLET_FOLDER, H.get(f)), Blob.class);
                }
                if (B.containsKey(f)) {
                    merge = Utils.readObject(Utils.join(GITLET_FOLDER, B.get(f)), Blob.class);
                }
                File replace = Utils.join(CWD, f);
                if (replace.exists()) {
                    try {
                        replace.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String newCont = "<<<<<<< HEAD\r\n";
                if (cur != null) {
                    newCont += cur.getcontentsasString();
                    if (newCont.charAt(newCont.length() - 1) != '\n') {
                        newCont += "\r\n";
                    }
                }
                newCont += "=======\r\n";
                if (merge != null) {
                    newCont += merge.getcontentsasString();
                    if (newCont.charAt(newCont.length() - 1) != '\n') {
                        newCont += "\r\n";
                    }
                }
                newCont += ">>>>>>>\r\n";
                Utils.writeContents(replace,newCont);
                StagingArea.add(replace);
                System.out.println("Encountered a merge conflict.");
            }
        }
        addCommit("Merged " + args[1] + " into " + _info.get("Active Branch") + ".", m.get_hashCode());
        save();
    }

    /** Helper function for merge, returns whether a file is absent or not. */
    public static boolean isAbsent(String fileName, Commit commit) {
        return !commit.containsFile(fileName) || !StagingArea.isStaged(fileName);
    }

    /** Contains branch and branch head information. */
    private static HashMap<String, String> _info;

    /** HashMap with keys as ID's and values as commits to access in O(N) time. */
    private static HashMap<String, Commit> _commithistory;

}