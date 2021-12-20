package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/** Enables remote capabilities for gitlet.
 * @author Jake Clayton*/
public class Remote {

    /** The command working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory for all persistence. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** The file containing remote login info. */
    static final File REMOTE = Utils.join(GITLET_FOLDER, "remotelogins");

    /** Initializes appropriate persistence. */
    public static void initialize() {
        if (!REMOTE.exists()) {
            try {
                REMOTE.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _loginInfo = new HashMap<String, String>();
        save();
    }

    /** Saves login information. */
    private static void save() {
        Utils.writeObject(REMOTE, _loginInfo);
    }

    /** Retrieves saved login information properly. */
    @SuppressWarnings("unchecked")
    private static void update() {
        _loginInfo = Utils.readObject(REMOTE, HashMap.class);
    }

    /** Saves the given login information under the given remote name.
     * @param args has the remote name and file path. */
    public static void addRemote(String... args) {
        update();
        if (_loginInfo.containsKey(args[1])) {
            Main.error("A remote with that name already exists.");
        }
        String path = args[2];
        path.replace("/", File.separator);
        _loginInfo.put(args[1], args[2]);
        save();
    }

    /** Remove information associated with the given remote name.
     * @param args has the remote name to be removed. */
    public static void rmRemote(String... args) {
        update();
        if (!_loginInfo.containsKey(args[1])) {
            Main.error("A remote with that name does not exist.");
        }
        _loginInfo.remove(args[1]);
        save();
    }

    /** Attempts to append the current branch's commits to the end
     * of the given branch at the given remote.
     * @param args args[1] is the remote name and args[2] is the branch. */
    @SuppressWarnings("unchecked")
    public static void push(String... args) {
        update();
        if (!_loginInfo.containsKey(args[1])) {
            Main.error("Remote directory not found.");
        }
        File remoteGit = new File(_loginInfo.get(args[1]));
        if (!remoteGit.exists()) {
            Main.error("Remote directory not found.");
        }
        File r = Utils.join(remoteGit, "commit_history");
        File i = Utils.join(remoteGit, "info");
        File stagingInfo = Utils.join(remoteGit, "staging");
        HashMap<String, Commit> rH = Utils.readObject(r, HashMap.class);
        HashMap<String, String> rI = Utils.readObject(i, HashMap.class);
        HashMap<String, Commit> localHistory = CommitTree.getHistory();
        Commit lHead = CommitTree.getHead();
        Commit rHead;
        if (rI.containsKey(args[2])) {
            rHead = rH.get(rI.get(args[2]));
        } else {
            rI.put(args[2], rI.get("Active Head"));
            rHead = rH.get(rI.get("Active Head"));
        }
        boolean contains = false;
        for (Commit c : localHistory.values()) {
            if (c.getMessage().equals(rHead.getMessage())) {
                contains = true;
            }
        }
        if (!contains) {
            Main.error("Please pull down remote changes before pushing.");
        }
        Commit end = null;
        Commit check = lHead;
        while (true) {
            if (rHead.getMessage().equals(check.getMessage())) {
                end = check;
            }
            if (check.getParent() == null) {
                break;
            }
            check = localHistory.get(check.getParent());
        }
        if (end == null) {
            Main.error("Please pull down remote changes before pushing.");
        }
        File stagedAdd = Utils.join(stagingInfo, "addition");
        for (File del : stagedAdd.listFiles()) {
            del.delete();
        }
        File stagedRem = Utils.join(stagingInfo, "removal");
        for (File del : stagedRem.listFiles()) {
            del.delete();
        }
        rI.put(rI.get("Active Branch"), rI.get("Active Head"));
        rI.replace("Active Head", lHead.getSha1());
        Utils.writeObject(r, localHistory);
        Utils.writeObject(i, rI);
    }



    /** Brings down commits from the remote Gitlet repository
     * into the local Gitlet repository.
     * @param args */
    @SuppressWarnings("unchecked")
    public static void fetch(String... args) {
        update();
        if (!_loginInfo.containsKey(args[1])) {
            Main.error("Remote directory not found.");
        }
        File remoteGit = new File(_loginInfo.get(args[1]));
        if (!remoteGit.exists()) {
            Main.error("Remote directory not found.");
        }
        File rI = Utils.join(remoteGit, "info");
        File cH = Utils.join(remoteGit, "commit_history");
        File lInfo = Utils.join(GITLET_FOLDER, "info");
        File lHistory = Utils.join(GITLET_FOLDER, "commit_history");
        HashMap<String, String> localInfo = CommitTree.getInfo();
        HashMap<String, Commit> localHistory = CommitTree.getHistory();
        HashMap<String, String> bI = Utils.readObject(rI, HashMap.class);
        HashMap<String, Commit> rH = Utils.readObject(cH, HashMap.class);
        Commit rHead = null;
        if (!bI.containsKey(args[2])) {
            if (!bI.get("Active Branch").equals(args[2])) {
                Main.error("That remote does not have that branch.");
            } else {
                rHead = rH.get(bI.get("Active Head"));
            }
        } else {
            rHead = rH.get(bI.get(args[2]));
        }
        Commit temp = rHead;
        while (true) {
            boolean addd = true;
            for (Commit c : localHistory.values()) {
                if (c.getMessage().equals(temp.getMessage())) {
                    addd = false;
                }
            }
            if (addd) {
                localHistory.put(temp.getSha1(), temp);
            }
            for (String sha1 : temp.contents().values()) {
                File f = Utils.join(remoteGit, sha1);
                Blob copyFrom = Utils.readObject(f, Blob.class);
                File add = Utils.join(GITLET_FOLDER, sha1);
                try {
                    add.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Utils.writeObject(add, copyFrom);
            }
            if (temp.getParent() == null) {
                break;
            }
            temp = rH.get(temp.getParent());
        }
        localInfo.put(args[1] + "/" + args[2], rHead.getSha1());
        StagingArea.clear();
        Utils.writeObject(lHistory, localHistory);
        Utils.writeObject(lInfo, localInfo);
    }

    /** Fetches branch [remote name]/[remote branch name] as for the fetch
     * command, and then merges that fetch into the current branch.
     @param args the input arguments */
    public static void pull(String... args) {
        fetch("fetch", args[1], args[2]);
        CommitTree.merge("merge", args[1] + "/" + args[2]);
    }

    /** The saved remote login info. */
    private static HashMap<String, String> _loginInfo;
}

