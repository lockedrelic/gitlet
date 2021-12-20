package gitlet;

import java.io.File;
import java.util.Arrays;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jake Clayton */
public class Main {

    /** The command working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory for all persistence. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            error("Please enter a command.");
        }
        if (args[0].equals("init")) {
            if (args.length == 1) {
                setupDirectory();
                return;
            } else {
                error("Incorrect operands.");
            }
        } else if (!GITLET_FOLDER.exists()) {
            error("Not in an initialized Gitlet directory.");
        } else if (args[0].equals("add")) {
            add(args);
        } else if (args[0].equals("commit")) {
            commit(args);
        } else if (args[0].equals("rm")) {
            remove(args);
        } else if (args[0].equals("log")) {
            log(args);
        } else if (args[0].equals("global-log")) {
            globalLog(args);
        } else if (args[0].equals("find")) {
            CommitTree.find(args);
        } else if (args[0].equals("status")) {
            printStatus();
        } else if (args[0].equals("checkout")) {
            checkout(args);
        } else if (args[0].equals("branch")) {
            branch(args);
        } else if (args[0].equals("rm-branch")) {
            rmBranch(args);
        } else if (args[0].equals("reset")) {
            CommitTree.reset(args);
        } else if (args[0].equals("merge")) {
            CommitTree.merge(args);
        } else if (args[0].equals("add-remote")) {
            addRemote(args);
        } else if (args[0].equals("rm-remote")) {
            rmRemote(args);
        } else if (args[0].equals("push")) {
            push(args);
        } else if (args[0].equals("fetch")) {
            fetch(args);
        } else if (args[0].equals("pull")) {
            Remote.pull(args);
        } else {
            error("No command with that name exists.");
        }
    }

    /** Returns the .gitlet directory file path. */
    public static File getDirectory() {
        return GITLET_FOLDER;
    }
    /** Initializes the .gitlet directory. */
    public static void setupDirectory() {
        if (GITLET_FOLDER.exists()) {
            String m = "A Gitlet version-control system already exists";
            m += " in the current directory.";
            error(m);
        }
        GITLET_FOLDER.mkdir();
        Remote.initialize();
        StagingArea.initialize();
        CommitTree.initCommit();
    }

    /** Directs the add command to StagingArea.
     * @param args */
    public static void add(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        File current = Utils.join(CWD, new File(args[1]).getName());
        StagingArea.add(current);
    }

    /** Directs the commit command to CommitTree.
     * @param args */
    public static void commit(String... args) {
        if (args.length == 1 || args[1].length() == 0) {
            error("Please enter a commit message.");
        } else if (args.length != 2) {
            error("Incorrect operands.");
        } else if (StagingArea.isEmpty()) {
            error("No changes added to the commit.");
        }
        CommitTree.addCommit(args[1], null);
    }

    /** Directs the branch command to CommitTree.
     * @param args */
    public static void branch(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        CommitTree.branch(args[1]);
    }

    /** Directs the rm-branch command to CommitTree.
     * @param args */
    public static void rmBranch(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        CommitTree.rmBranch(args[1]);
    }

    /** Directs the remove command to StagingArea.
     * @param args */
    public static void remove(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        StagingArea.rm(args[1]);
    }

    /** Directs the log command to class CommitTree.
     * @param args */
    public static void log(String... args) {
        if (args.length != 1) {
            error("Incorrect operands");
        }
        CommitTree.printCommitHistory();
    }

    /** Directs the global-log command to class CommitTree.
     * @param args */
    public static void globalLog(String... args) {
        if (args.length != 1) {
            error("Incorrect operands");
        }
        CommitTree.printAllCommits();
    }

    /** Directs the checkout command to class CommitTree.
     * @param args */
    public static void checkout(String... args) {
        if (args.length < 2) {
            error("Incorrect operands");
        }
        CommitTree.checkout(Arrays.copyOfRange(args, 1, args.length));
    }

    /** Prints out MESSAGE and exits with error code 0.
     * @param message */
    public static void error(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    /** Directs the status command to StagingArea. */
    public static void printStatus() {
        StagingArea.printStatus();
    }

    /** Directs the add-remote command to Remote.
     * @param args */
    public static void addRemote(String... args) {
        Remote.addRemote(args);
    }

    /** Directs the rm-remote command to Remote.
     * @param args  */
    public static void rmRemote(String... args) {
        Remote.rmRemote(args);
    }

    /** Directs the push command to Remote.
     * @param args */
    public static void push(String... args) {
        Remote.push(args);
    }

    /** Directs the fetch command to remote.
     * @param args */
    public static void fetch(String... args) {
        Remote.fetch(args);
    }

}

