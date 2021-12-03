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
        } else if (!GITLET_FOLDER.exists()){
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
            if (args.length != 2) {
                error("Incorrect operands.");
            }
            CommitTree.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            if (args.length != 2) {
                error("Incorrect operands.");
            }
            CommitTree.rm_branch(args[1]);
        } else if (args[0].equals("reset")) {
            CommitTree.reset(args);
        } else if (args[0].equals("merge")) {
            CommitTree.merge(args);
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
            error("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_FOLDER.mkdir();
        StagingArea.initialize();
        CommitTree.initCommit();
    }

    /** Directs the add command to StagingArea. */
    public static void add(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        File current = Utils.join(CWD, new File(args[1]).getName());
        StagingArea.add(current);
    }

    /** Directs the commit command to CommitTree. */
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

    /** Directs the remove command to StagingArea. */
    public static void remove(String... args) {
        if (args.length != 2) {
            error("Incorrect operands.");
        }
        StagingArea.rm(args[1]);
    }

    /** Directs the log command to class CommitTree. */
    public static void log(String... args) {
        if (args.length != 1) {
            error("Incorrect operands");
        }
        CommitTree.printCommitHistory();
    }

    /** Directs the global-log command to class CommitTree. */
    public static void globalLog(String... args) {
        if (args.length != 1) {
            error("Incorrect operands");
        }
        CommitTree.printAllCommits();
    }

    /** Directs the checkout command to class CommitTree. */
    public static void checkout(String... args) {
        if (args.length < 2) {
            error("Incorrect operands");
        }
        CommitTree.checkout(Arrays.copyOfRange(args, 1, args.length));
    }

    /** Prints out MESSAGE and exits with error code 0. */
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

}