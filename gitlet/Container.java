package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;


public class Container implements Serializable {
    /* The commit object which the current branch points to. */
    public Commit currCommit;

    /* current branch */
    public String currBranch;

    /* Maps branch name to its head commit. */
    public Map<String, Commit> branchMap;

    /* An arraylist of committed objects. */
    public ArrayList<Commit> committed;

    /* Maps SHA-1 to commit object. */
    public Map<String, Commit> commitMap;

    /* Maps file name of staged files to SHA-1. */
    public Map<String, String> stagingArea;

    /* Maps SHA-1 to file name. */
    public Map<String, String> shaNameMap;

    /* A set of file names that are untracked. */
    public Set<String> untracked;

    public Commit firstCommit;

    /* Constructs the ArrayList committed with an initial branch name (master);
     * add the initial commit object to it;
     * modify branchMap and currPointer. */
    public Container(Commit initCommit) {
        this.firstCommit = initCommit;
        this.committed = new ArrayList<Commit>();
        this.committed.add(initCommit);
        this.branchMap = new HashMap<String, Commit>();
        this.branchMap.put("master", initCommit);
        this.currCommit = initCommit;
        this.currBranch = "master";
        this.commitMap = new HashMap<>();
        this.stagingArea = new HashMap<>();
        this.shaNameMap = new HashMap<>();
        this.untracked = new HashSet<>();
    }

    /* Returns the current pointer. */
    public Commit getCurrPointer() {
        return currCommit;
    }

    /* Returns a list of branches. */
    public Set<String> getBranches() {
        return branchMap.keySet();
    };

    /* Returns the list of committed files represented by SHA. */
    public ArrayList<Commit> getCommitted() {
        return committed;
    }

    /* Returns the map of staged files. */
    public Map<String, String> getStaged() {
        return stagingArea;
    };

    /* Add a new branch with specified name. */
    public void addBranch(String name) {
        branchMap.put(name, branchMap.get(getCurrPointer()));
    };

    /* Removes the specified branch. */
    public void rmBranch(String name) {
        branchMap.remove(name);
    }

    /* Change to the specified branch. */
    public void changeBranch(String name) {
        currCommit = branchMap.get(name);
        currBranch = name;
    }

    /* [git add] Stage the file. */
    public void stage(File file) {
        if (file != null) {
            stagingArea.put(file.getName(), Utils.sha1(Utils.readContents(file)));
        }
    }

    /* Unstage the file. */
    public void unstage(File file) {
        stagingArea.remove(file.getName());
    }

    /* Retrack the file. */
    public void retrack(String fileName) {
        untracked.remove(fileName);
    }

    /* Untrack the file from the staging area. */
    public void untrack(String fileName) {
        stagingArea.remove(fileName);
        untracked.add(fileName);
    }

    /* Return true is the file is tracked. */
    public boolean tracking(String filename) {
        if (untracked == null) return true;
        return !untracked.contains(filename);
    }

    public boolean staged(String filename) {
        return stagingArea.containsKey(filename);
    }

    public Map<String, String> getTracked() {
        List<Commit> headCommit = new ArrayList();
        for (String s: branchMap.keySet()) {
            headCommit.add(branchMap.get(s));
        }
        Collections.sort(headCommit, (o1, o2) -> o1.getTimeStamp().compareTo(o2.getTimeStamp()));
        Map<String, String> mapF = new HashMap();
        for (Commit com: headCommit) {
            for (String s: com.filesMap.keySet()) {
                mapF.put(s, com.filesMap.get(s));
            }
        }
        return mapF;
    }

    /* Find the split point of two Commit objects,
     * assuming that the two commit objects are in different
     * branches and are not necessarily first cousins. */
    public Commit ancestor2(Commit c1, Commit c2) {
        Commit tempC1 = c1;
        Commit tempC2 = c2;
        int count1 = 0;
        int count2 = 0;
        while (tempC1.parent != null) {
            count1 += 1;
            tempC1 = tempC1.parent;
        }
        while (tempC2.parent != null) {
            count2 += 1;
            tempC2 = tempC2.parent;
        }
        tempC1 = c1;
        tempC2 = c2;
        if (count1 < count2) {
            for (int i = 0; i < count2 - count1; i++) {
                tempC2 = tempC2.parent;
            }
            if (tempC2.id.equals(c1.id)) {
                return c1;
            }
        } else if (count1 > count2) {
            for (int i = 0; i < count1 - count2; i++) {
                tempC1 = tempC1.parent;
            }
            if (tempC1.id.equals(c2.id)) {
                return c2;
            }
        }
        while (c1.parent != null) {
            if (c1.parent.id.equals(c2.parent.id)) {
                return c1.parent;
            }
            c1 = c1.parent;
            c2 = c2.parent;
        }
        return firstCommit;
    }

    // checks if ancestor is an ancestor of curr
    public boolean isAncestor(Commit curr, Commit ancestor) {
        while (curr != null) {
            Commit parent = curr.parent;
            if (parent != null && parent.id.equals(ancestor.id)) return true;
            curr = parent;
        }
        return false;
    }
}
