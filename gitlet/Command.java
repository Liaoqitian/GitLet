package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Command {
    /* An array of string that contains parameters from terminal input. */
    public String[] argu;
    /* Represents the specific command to call. */
    public String commandor;
    /* An immutable file that stands as gitlet's 'home.' */
    public static final File GITLETDIR = new File(new File(
            System.getProperty("user.dir")), ".gitlet");
    /* Where the container lives. */
    public static File containerDir = new File(GITLETDIR, "container");
    /* A file that stores blobs in the staging area. */
    public static File saDir = new File(GITLETDIR, "sa");

    /* Command constructor, takes in arguments (terminal input) and split command & parameter. */
    public Command(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("No command entered.");
        }
        commandor = args[0];
        if (args.length > 1) {
            argu = Arrays.copyOfRange(args, 1, args.length);
        } else {
            argu = null;
        }
    }

    public Container execute(Container container) {
        if (commandor.equals("init")) {
            if (argu != null) {
                throw new IllegalArgumentException();
            }
            container = init(container);
        } else if (commandor.equals("add")) {
            if (argu == null) {
                throw new IllegalArgumentException();
            }
            add(container);
        } else if (commandor.equals("commit")) {
            if (argu == null || (argu != null && argu.length != 1) || argu[0].equals("")) {
                throw new IllegalArgumentException("Please enter a commit message.");
            }
            commit(container);
        } else if (commandor.equals("rm")) {
            if (argu == null) {
                throw new IllegalArgumentException();
            }
            remove(container);
        } else if (commandor.equals("log")) {
            if (argu != null) {
                throw new IllegalArgumentException();
            }
            log(container);
        } else if (commandor.equals("global-log")) {
            if (argu != null) {
                throw new IllegalArgumentException();
            }
            globallog(container);
        } else if (commandor.equals("find")) {
            if (argu == null || (argu != null && argu.length != 1)) {
                throw new IllegalArgumentException();
            }
            find(container);
        } else if (commandor.equals("status")) {
            if (argu != null) {
                throw new IllegalArgumentException();
            }
            status(container);
        } else if (commandor.equals("checkout")) {
            if (argu == null || (argu != null && argu.length > 3)) {
                throw new IllegalArgumentException();
            }
            checkout(container);
        } else if (commandor.equals("branch")) {
            if (argu == null || (argu != null && argu.length != 1)) {
                throw new IllegalArgumentException();
            }
            branch(container);
        } else if (commandor.equals("rm-branch")) {
            if (argu == null || (argu != null && argu.length != 1)) {
                throw new IllegalArgumentException();
            }
            rmbranch(container);
        } else if (commandor.equals("reset")) {
            if (argu == null) {
                throw new IllegalArgumentException();
            }
            reset(container);
        } else if (commandor.equals("merge")) {
            if (argu == null || (argu != null && argu.length != 1)) {
                throw new IllegalArgumentException();
            }
            merge(container);
        }
        return container;
    }

    /* Creates a container object*/
    public Container init(Container container) {
        /* Append "/.gitlet" to whatever path was created above. */
        if (Command.GITLETDIR.exists()) {
            System.out.println("A gitlet version-control system already "
                    + "exists in the current directory");
        } else {
            /* Create the .gitlet directory */
            saDir.mkdirs();
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String id = Utils.sha1("initial commit", currentTime);
            Commit initial = new Commit("initial commit", currentTime,
                    null, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), id);
            container = new Container(initial);
            container.commitMap.put(id, initial);
        }
        return container;
    }

    public void add(Container container) {
        for (String s : argu) {
            /* Append the file to the staging area directory */
            File currDir = new File(System.getProperty("user.dir"));
            File workingDFile = new File(currDir, s);
            if (!workingDFile.exists()) {
                System.out.println("File does not exist.");
            } else {
                /* If the file is untracked, delete the mark from set untracked. */
                if (!container.tracking(s)) {
                    container.retrack(s);
                }
                container.stage(workingDFile);
                /* Check if the added version is identical to the version in current commit. */
                if (container.currPointer.filesMap.containsKey(s)
                        && container.currPointer.latestID(s).equals(container.stagingArea.get(s))) {
                    container.stagingArea.remove(s);
                } else {
                    byte[] contentByte = Utils.readContents(workingDFile);
                    File saFile = new File(saDir, container.stagingArea.get(s));
                    try {
                        saFile.createNewFile(); // Access the file in working directory
                        Utils.writeContents(saFile, contentByte);
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }
    }

    /* Construct a new ArrayList of files for the upcoming commit. */
    /* Construct a copy of the ArrayList of files of its parent. */
    public void commit(Container container) {
        if (container.stagingArea.isEmpty() && container.recentUntracked.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        ArrayList<File> newFiles = new ArrayList<>();
        if (container.currPointer.files != null) {
            for (int index = 0; index < container.currPointer.files.size(); index += 1) {
                if (container.recentUntracked.contains(container.currPointer.files.get(index))) {
                    newFiles.add(index, container.currPointer.files.get(index));
                }
            }
            List<String> newFilesName = new ArrayList<>();
            for (int i = 0; i < newFiles.size(); i++) {
                newFilesName.add(i, newFiles.get(i).getName());
            }
            for (String filename : container.stagingArea.keySet()) {
                try {
                    File archive = commitHelper(container, filename);
                    if (newFilesName.contains(filename)) {
                        newFiles.remove(new File(GITLETDIR,
                                container.currPointer.filesMap.get(filename)));
                    }
                    newFiles.add(archive);
                    container.shaNameMap.put(container.stagingArea.get(filename), filename);

                } catch (IOException e) {
                    return;
                }
            }
        } else if (container.currPointer.files == null) {
            for (String filename : container.stagingArea.keySet()) {
                try {
                    File archive = commitHelper(container, filename);
                    newFiles.add(archive);
                    container.shaNameMap.put(container.stagingArea.get(filename), filename);
                } catch (IOException e) {
                    return;
                }
            }
        }
        Map<String, String> newFilesMap = new HashMap<>();
        for (String filename: container.currPointer.filesMap.keySet()) {
            if (!container.stagedUntracked.contains(filename)) {
                newFilesMap.put(filename, container.currPointer.filesMap.get(filename));
            }
        }
        for (String filename: container.stagingArea.keySet()) {
            newFilesMap.put(filename, container.stagingArea.get(filename));
        }
        int ind = container.committed.size() - 1;
        ArrayList<String> sID = new ArrayList<>();
        ArrayList<Commit> sibCommit = new ArrayList<>();
        Commit temp = container.committed.get(ind);
        while ((!temp.id.equals(container.currPointer.id)) && ind > 0) {
            if (temp.parent.id.equals(container.currPointer.id)) {
                sID.add(temp.id);
                sibCommit.add(temp);
            }
            ind -= 1;
            temp = container.committed.get(ind);
        }
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String id = Utils.sha1(argu[0], currentTime, container.currPointer.id);
        Commit currCommit = new Commit(argu[0], currentTime,
                container.currPointer, sID, newFiles, newFilesMap, id);
        for (Commit sisterCommit : sibCommit) {
            sisterCommit.sibling.add(currCommit.id);
        }
        container.currPointer = currCommit;
        container.committed.add(currCommit);
        container.branchMap.put(container.currBranch, currCommit);
        container.stagingArea = new HashMap<>();
        container.recentUntracked = new HashSet<>();
        container.commitMap.put(currCommit.id, currCommit);
        File[] fileList = saDir.listFiles();
        for (File f : fileList) {
            f.delete();
        }
    }

    public File commitHelper(Container container, String filename) throws IOException {
        File saFile = new File(saDir, container.stagingArea.get(filename));
        byte[] saByte = Utils.readContents(saFile);
        File archive = new File(GITLETDIR, container.stagingArea.get(filename));
        archive.createNewFile();
        Utils.writeContents(archive, saByte);
        return archive;
    }

    /* If the file is neither in stagingArea nor tracked by the current commit, print error message
     * if it is tracked by the current commit, untrack it and delete the file from repository;
     * if it is staged, unstage it. */
    public void remove(Container container) {
        for (String filename : argu) {
            File currFile = new File(filename);
            Boolean trackedByCurrCommit = container.currPointer.filesMap.containsKey(filename);
            if (!container.staged(filename) && !trackedByCurrCommit) {
                System.out.println("No reason to remove the file.");
            }
            if (trackedByCurrCommit) {
                container.untrack(filename);
                Utils.restrictedDelete(currFile);
            }
            /* Note that the unstage method doesn't error even if the file is not unstaged.*/
            container.unstage(currFile);
        }
    }

    /* Show information each commit backwards along the commit
     * tree from the current commit to the initial commit. */
    public void log(Container container) {
        Commit pointer = container.currPointer;
        while (!pointer.equals(container.firstCommit)) {
            System.out.println("===");
            pointer.print();
            System.out.println();
            pointer = pointer.parent;
        }
        System.out.println("===");
        container.firstCommit.print();
    }

    public void globallog(Container container) {
        for (Commit com : container.getCommitted()) {
            System.out.println("===");
            System.out.println("Commit " + com.getID());
            System.out.println(com.getTimeStamp());
            System.out.println(com.getMessage());
            System.out.println();
        }
    }

    /* Prints out the ids of all commits that have the given commit message. */
    public void find(Container container) {
        /* Filter out the commits. */
        List<Commit> correspCommit = container.getCommitted().stream()
                .filter(o -> o.message.equals(argu[0])).collect(Collectors.toList());
        if (correspCommit.size() == 0) {
            System.out.println("Found no commit with that message.");
            return;
        }
        for (Commit com : correspCommit) {
            System.out.println(com.id);
        }
    }

    /* Show the branches, staged files, and removed files. */
    public void status(Container container) {
        /* Print out all branches;
         * the current branch has an asterisk in front of it. */
        System.out.println("=== Branches ===");
        String currB = container.currBranch;
        Set<String> statusBranches = container.branchMap.keySet();
        List<String> orderedBranches = statusBranches.stream().
                sorted((o1, o2) -> o1.compareTo(o2)).collect(Collectors.toList());
        for (String s : orderedBranches) {
            if (s.equals(currB)) {
                System.out.println("*" + s);
            } else {
                System.out.println(s);
            }
        }
        System.out.println();

        /* Print out the staged files. */
        System.out.println("=== Staged Files ===");
        if (container.stagingArea != null) {
            Set<String> stagedFiles = container.stagingArea.keySet();
            List<String> orderedStaged = stagedFiles.stream().
                    sorted((o1, o2) -> o1.compareTo(o2)).collect(Collectors.toList());
            for (String s : orderedStaged) {
                System.out.println(s);
            }
        }
        System.out.println();
        /* Print out the removed files. */
        System.out.println("=== Removed Files ===");
        if (container.recentUntracked != null) {
            List<String> orderedRemoved = new ArrayList<>(container.recentUntracked);
            Collections.sort(orderedRemoved);
            for (String s : orderedRemoved) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /* Add a new branch which points to the current head commit.
     * However, it does not immediately switch to the newly created branch;
     * print error message if new branch name already exists. */
    public void branch(Container container) {
        if (container.branchMap.keySet().contains(argu[0])) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        container.branchMap.put(argu[0], container.currPointer);
    }

    /* Remove the branch pointer (delete it from the branchMap);
     * prints error message if the branch name doesn't exist
     * or the indicated branch is the current branch. */
    public void rmbranch(Container container) {
        if (!container.branchMap.keySet().contains(argu[0])) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (container.currBranch.equals(argu[0])) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        container.branchMap.remove(argu[0]);
    }

    /* checkout -- [file name]: Takes the version of the file as it exists
     * in the head commit, puts it in the working directory,
     * overwriting the version of the file that is already there if there is one.
     * The new version of the file should not be staged. */
    public void checkout1(Container container) throws IOException {
        if (!container.currPointer.filesMap.containsKey(argu[1])) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File toCheckout = null;
        for (String filename : container.currPointer.filesMap.keySet()) {
            if (filename.equals(argu[1])) {
                // toCheckout = Utils.join(GITLETDIR, container.currPointer.filesMap.get(filename));
                toCheckout = new File(GITLETDIR, container.currPointer.filesMap.get(filename));
                break;
            }
        }
        if (toCheckout != null) {
            container.unstage(toCheckout);

            // get current directory
            File curDir = new File(System.getProperty("user.dir"));

            // append current file name to the working directory
            File checkedFile = new File(curDir, argu[1]);

            // create a new empty file inside current new file
            checkedFile.createNewFile();

            // copy contents over to the empty file
            byte[] content = Utils.readContents(toCheckout);
            Utils.writeContents(checkedFile, content);
        }
    }

    /* checkout [commit id] -- [file name]: Takes the version of the file as
     * it exists in the commit with the given id(find id in the map),
     * and puts it in the working directory,
     * overwriting the version of the file that is already there if there is one.
     * The new version should not be staged.*/
    public void checkout2(Container container) throws IOException {
        String id = argu[0];
        String fileName = argu[2];

        if (id.length() < 6 || id.length() > 40) {
            System.out.println("No commit with that id exists.");
            return;
        }

        for (String sha : container.commitMap.keySet()) {
            if (sha.substring(0, id.length()).equals(id)) {
                id = sha;
            }
        }

        if (container.commitMap.get(id) == null
                || !container.commitMap.keySet().contains(id)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        if (container.commitMap.get(id).filesMap.isEmpty()
                || !container.commitMap.get(id).filesMap.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        File toCheckout = null;
        for (String filename : container.commitMap.get(id).filesMap.keySet()) {
            if (filename.equals(argu[2])) {
                toCheckout = new File(GITLETDIR,
                        container.commitMap.get(id).filesMap.get(filename));
                break;
            }
        }

        if (toCheckout != null) {
            //File workFile = Utils.writeContents(currFile, Utils.readContents(result)); // HELP
            container.unstage(toCheckout);

            // get current directory
            File curDir = new File(System.getProperty("user.dir"));

            // append current file name to the working directory
            File checkedFile = new File(curDir, argu[2]);

            // create a new empty file inside current new file
            checkedFile.createNewFile();

            // copy contents over to the empty file
            byte[] content = Utils.readContents(toCheckout);
            Utils.writeContents(checkedFile, content);
        }
    }


    /* checkout [branch name]:
    1. Takes all files and puts them in the working
        directory, overwriting the old versions
    2. at the end of this command, the given branch
        will be the current branch (HEAD).
    3. Any files that are tracked in the current branch
        but are not present in the checked-out branch are deleted.
    4. The staging area is cleared, unless the
        checked-out branch is the current branch
    */
    public void checkout3(Container container) throws IOException {
        String branchName = argu[0];
        if (!container.branchMap.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (container.branchMap.get(branchName).equals(container.currPointer)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File curDir = new File(System.getProperty("user.dir"));
        File[] fileList = curDir.listFiles();
        ArrayList<String> wDname = new ArrayList<>();
        for (File f : fileList) {
            wDname.add(f.getName());
        }
        for (String fName : container.branchMap.get(branchName).filesMap.keySet()) {
            if (!container.currPointer.filesMap.containsKey(fName) && wDname.contains(fName)
                    && (!container.branchMap.get(branchName).filesMap.get(fName)
                    .equals(Utils.sha1(Utils.readContents(new File(curDir, fName)))))) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it or add it first.");
                return;
            }
        }
        for (String fName : container.currPointer.filesMap.keySet()) {
            if (wDname.contains(fName) && !container.branchMap
                    .get(branchName).filesMap.containsKey(fName)) {
                File toDelete = new File(curDir, fName);
                toDelete.delete();
            }
        }
        for (String a : container.branchMap.get(branchName).filesMap.keySet()) {
            File toUnStage = new File(GITLETDIR, container.branchMap.
                    get(branchName).filesMap.get(a));
            // append current file name to the working directory
            File checkedFile = new File(curDir, a);
            // create a new empty file inside current new file
            checkedFile.createNewFile();
            // copy contents over to the empty file
            byte[] content = Utils.readContents(toUnStage);
            Utils.writeContents(checkedFile, content);
        }
        // clear staging area, unless the checked-out branch is the current branch
        if (container.branchMap.get(branchName) != container.branchMap.get(container.currBranch)) {
            container.stagingArea.clear();
        }
        // set given branch to current branch
        container.currPointer = container.branchMap.get(branchName);
        container.currBranch = branchName;
    }



    /* Check-out implementation, checks out file based on specified command */

    public void checkout(Container container) {
        try {
            if (argu.length == 1) {
                checkout3(container);
            } else if (argu.length == 2 && argu[0].equals("--")) {
                checkout1(container);
            } else if (argu.length == 3 && argu[1].equals("--")) {
                checkout2(container);
            } else {
                System.out.println("Incorrect operands.");
                return;
                //throw new IllegalArgumentException();
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }

    }


    /*  reset [commit id]:
    1. Check out all the files tracked by the given commit.
    2. Remove tracked files that are not present in the given commit.
    3. Moves the current branchÃ¢â‚¬â„¢s head pointer and the head pointer to that commit node.

    The [commit id] may be abbreviated as for checkout. The staging area is cleared.
    The command is essentially checkout of an arbitrary commit
    that also changes the current branch head pointer.*/
    public void reset(Container container) {
        String id = argu[0];
        int len = id.length();
        Commit correspCommit = null;
        if (len < 40 && len >= 6) {
            for (String shaL : container.commitMap.keySet()) {
                String shaS = shaL.substring(0, len);
                if (shaS.equals(id)) {
                    correspCommit = container.commitMap.get(shaL);
                    break;
                }
            }
        }
        if (!container.commitMap.containsKey(id) && correspCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        } else if (container.commitMap.containsKey(id)) {
            correspCommit = container.commitMap.get(id);
        }
        // remove tracked files not present in the given commit
        // check out files in the given commit
        // move head pointer to the given commit
        container.branchMap.put(container.currBranch, correspCommit);
        Command newCommand = new Command(new String[]{"checkout", container.currBranch});
        newCommand.execute(container);
        container.currPointer = correspCommit;
        container.branchMap.put(container.currBranch, container.currPointer);
        container.stagingArea.clear();
    }



   /*  merge [branch name]:
    Merges files from the given branch into the current branch
    1. If the split point is the same commit as the given branch,
        then we do nothing; the merge is complete, and the operation ends
        with the message "Given branch is an ancestor of the current branch."
    2. If the split point is the current branch, then the current branch
    is set to the same commit as the given branch and the operation ends after
        printing the message "Current branch fast-forwarded."
    3. Otherwise follows the rules blow(since the split point):
           compare each commits after the split point and perform the following:
           1) Any files that have been modified in the given branch,
              but not modified in the current branch should be changed to
              their versions in the given branch.
           2) Any files that have been modified in the current branch but not
              in the given branch should stay as they are.
           3) Any files that were not present at the split point and are present
              only in the current branch should remain as they are
           4) Any files that were not present at the split point and are present
              only in the given branch should be checked out and staged.
           5) Any files present at the split point, unmodified
              in the current branch, and absent in the given branch
              should be removed.
           6) Any files present at the split point, unmodified
              in the given branch, and absent in the current branch
              should remain absent.
           7) Any files modified in different ways in
              the current and given branches are in conflict.
    */
    public boolean preMergeChecker(Commit other, String givenBranch, Container container) {
        if (other == null) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        if (givenBranch.equals(container.currBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }
        if (!container.stagingArea.isEmpty() || !container.recentUntracked.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        }
        return true;
    }
    public void merge(Container container) {
        String givenBranch = argu[0];
        Commit other = container.branchMap.get(givenBranch);
        ArrayList<String> modifiedOther = new ArrayList<>();
        ArrayList<String> modifiedMaster = new ArrayList<>();
        Boolean conflict = false;
        if (!preMergeChecker(other, givenBranch, container)) {
            return;
        }
        Commit splitPoint = container.ancestor2(container.currPointer, other);
        File curDir = new File(System.getProperty("user.dir"));
        File[] fileList = curDir.listFiles();
        ArrayList<String> wDname = new ArrayList<>();
        for (File f : fileList) {
            wDname.add(f.getName());
        }
        for (String fName : other.filesMap.keySet()) {
            if (!container.currPointer.filesMap.containsKey(fName) && wDname.contains(fName)
                    && (!other.filesMap.get(fName)
                    .equals(Utils.sha1(Utils.readContents(new File(curDir, fName)))))) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it or add it first.");
                return;
            }
        }
        if (splitPoint == other) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitPoint == container.currPointer) {
            container.currPointer = other;
            container.branchMap.put(container.currBranch, container.currPointer);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        mergeHelper(modifiedMaster, modifiedOther, other, splitPoint, container);
        for (String fname : modifiedOther) {
            if (other.filesMap.get(fname) != null) {
                if (modifiedMaster.contains(fname)) {
                    conflict = true;
                    conflictHelper(other.filesMap.get(fname),
                            container.currPointer.filesMap.get(fname), container, fname);
                } else {
                    try {
                        String sha = other.filesMap.get(fname);
                        container.stagingArea.put(fname, sha);
                        File saFile = new File(saDir, sha);
                        saFile.createNewFile();
                        File historyFile = new File(GITLETDIR, sha);
                        byte[] saByte = Utils.readContents(historyFile);
                        Utils.writeContents(saFile, saByte);
                        Command newCommand = new Command(new
                                String[]{"checkout", other.id, "--", fname});
                        newCommand.execute(container);
                    } catch (IOException e) {
                        return;
                    }
                }
            } else {
                if (modifiedMaster.contains(fname)) {
                    conflict = true;
                    conflictHelper(other.filesMap.get(fname),
                            container.currPointer.filesMap.get(fname), container, fname);
                }
                if (!modifiedMaster.contains(fname)) {
                    File currDir = new File(System.getProperty("user.dir"), fname);
                    container.untrack(fname);
                    currDir.delete();
                }
            }
        }
        if (!conflict) {
            Command newCommand = new Command(new String[]{"commit",
                "Merged " + container.currBranch + " with " + givenBranch + "."});
            newCommand.execute(container);
        } else {
            System.out.println("Encountered a merge conflict.");
            container.recentUntracked.clear();
        }
    }
    private void mergeHelper(ArrayList<String> katherine, ArrayList<String> amanda,
                             Commit qitian, Commit zuojun, Container david) {
        for (String fname : qitian.filesMap.keySet()) {
            if (!zuojun.filesMap.containsKey(fname)
                    || !qitian.filesMap.get(fname).equals(zuojun.filesMap.get(fname))) {
                amanda.add(fname);
            }
        }
        for (String fname : david.currPointer.filesMap.keySet()) {
            if (!zuojun.filesMap.keySet().contains(fname)
                    || !zuojun.filesMap.get(fname).equals(david.currPointer.filesMap.get(fname))) {
                katherine.add(fname);
            }
        }
        for (String fname : zuojun.filesMap.keySet()) {
            if (!qitian.filesMap.containsKey(fname)) {
                amanda.add(fname);
            }
        }
    }
    private void conflictHelper(String otherSha,
                                String currSha, Container container, String fname) {
        byte[] prefix = "<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8);
        byte[] sep = "=======\n".getBytes(StandardCharsets.UTF_8);
        byte[] postfix = ">>>>>>>\n".getBytes(StandardCharsets.UTF_8);
        byte[] v1 = new byte[0];
        byte[] v2 = new byte[0];
        File currDir = new File(System.getProperty("user.dir"), fname);

        File currFile = new File(GITLETDIR, currSha);
        v1 = Utils.readContents(currFile);
        if (otherSha != null) {
            File otherFile = new File(GITLETDIR, otherSha);
            v2 = Utils.readContents(otherFile);
        } else if (otherSha == null) {
            v2 = new byte[0];
        }

        byte[] combined = new byte[prefix.length + v1.length
                + sep.length + v2.length + postfix.length];

        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(v1, 0, combined, prefix.length, v1.length);
        System.arraycopy(sep, 0, combined, prefix.length + v1.length, sep.length);
        System.arraycopy(v2, 0, combined, prefix.length + v1.length + sep.length, v2.length);
        System.arraycopy(postfix, 0, combined, prefix.length
                + v1.length + sep.length + v2.length, postfix.length);

        //String hash = Util.sha1(prefix, v1, sep, v2, postfix);
        Utils.writeContents(currDir, combined);
        //Utils.writeContents(new File(saDir, fname), combined); // not sure
    }
}

