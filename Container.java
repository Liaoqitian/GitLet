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
    public Commit currPointer;

    /* The current branch */
    public String currBranch;

    /* A map that takes branch name and its head pointer. */
    public Map<String, Commit> branchMap;

    /* An arraylist of commit objects. */
    public ArrayList<Commit> committed;

    /* A map that takes SHA-1 of commit object. */
    public Map<String, Commit> commitMap;

    /* A map of filename and SHA-1 for staged files. */
    public Map<String, String> stagingArea;

    /* A map that links SHA-1 to actual file name. */
    public Map<String, String> shaNameMap;

    /* A set of name of files that is now untracked. */
    public Set<String> stagedUntracked;

    public Set<String> recentUntracked;

    public Commit firstCommit;

    /* Constructs the ArrayList committed with an initial branch name (master);
     * add the initial commit object to it;
     * modify branchMap and currPointer. */
    public Container(Commit initCommit) {
        this.firstCommit = initCommit;
        this.committed = new ArrayList<>();
        this.committed.add(initCommit);
        this.branchMap = new HashMap<>();
        this.branchMap.put("master", initCommit);
        this.currPointer = initCommit;
        this.currBranch = "master";
        this.commitMap = new HashMap<>();
        this.stagingArea = new HashMap<>();
        this.shaNameMap = new HashMap<>();
        this.stagedUntracked = new HashSet<>();
        this.recentUntracked = new HashSet<>();
    }

    /* Returns the current pointer. */
    public Commit getCurrPointer() {
        return currPointer;
    }

    /* Returns a list of branches. */
    public Set<String> getBranches() {

        return branchMap.keySet();
    };

    /* Returns the list of committed files represented by SHA. */
    public ArrayList<Commit> getCommitted() {
        return committed;
        // May need modification.
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
        currPointer = branchMap.get(name);
        currBranch = name;
    }

    /* [git add] Stage the file. */
    public void stage(File file) {
        if (file != null) {
            stagingArea.put(file.getName(), Utils.sha1(Utils.readContents(file)));
            // Construct a map whose key is the filename, and the value is it corresponding sha id.
        }
    }

    /* Unstage the file. */
    public void unstage(File file) {
        stagingArea.remove(file.getName());
    }

    /* Retrack the file. */
    public void retrack(String fileName) {

        stagedUntracked.remove(fileName);
        if (recentUntracked.contains(fileName)) {
            recentUntracked.remove(fileName);
        }
    }

    /* Untrack the file from the staging area. */
    public void untrack(String fileName) {
//        String toUnTrack = "";
//        for (String name: stagingArea.keySet()) {
//            if (name.equals(fileName)) {
//                toUnTrack = name;
//                break;
//            }
//        }
        stagingArea.remove(fileName);
        stagedUntracked.add(fileName);
        recentUntracked.add(fileName);
    }

    /* Return true is the file is tracked. */
    public boolean tracking(String filename) {
        if (stagedUntracked != null) {
            return !stagedUntracked.contains(filename);
        }
        return true;
    }

    public boolean staged(String filename) {
        return stagingArea.containsKey(filename);
    }

    public Map<String, String> getTracked() {
        List<Commit> headCommit = new ArrayList<Commit>();
        for (String s: branchMap.keySet()) {
            headCommit.add(branchMap.get(s));
        }
        Collections.sort(headCommit, (o1, o2) -> o1.getTimeStamp().compareTo(o2.getTimeStamp()));
        Map<String, String> mapF = new HashMap<String, String>();
        for (Commit com: headCommit) {
            for (String s: com.filesMap.keySet()) {
                mapF.put(s, com.filesMap.get(s));
            }
        }
        return mapF;
    }

    /* Find the split point of two Commit objects,
     * assuming that the two commit objects are in difference
     * branches and are not necessarily first cousins. */
    public Commit ancestor2(Commit c1, Commit c2) {
        Commit tempC1 = c1;
        Commit tempC2 = c2;
        int count1 = 0;
        int count2 = 0;
//        if(c1.parent.id.equals(c2.parent.id)) {
//            return c1.parent;
//        }
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



    /* Find the split point of two Commit objects,
     * assuming that the two commit objects are in difference
     * branches and are not necessarily first cousins. */
//    public Commit ancestor(Commit c1, Commit c2) {
//        Set<String> anSet1 = new HashSet<>();
//        Set<String> anSet2 = new HashSet<>();
//        Commit tempC1 = c1;
//        Commit tempC2 = c2;
//        Commit pa1 = null;
//        Commit pa2 = null;
//        if (ancestorHelper(tempC1) == null && ancestorHelper(tempC2) == null) {
//            if(committed.indexOf(tempC1) < committed.indexOf(tempC2)) {
//                return tempC1;
//            }
//            return tempC2;
//        }
//        while (ancestorHelper(tempC1) != null || ancestorHelper(tempC2) != null) {
//            pa1 = ancestorHelper(tempC1);
//            anSet1.add(pa1.id);
//            tempC1 = pa1;
//            if (anSet2.contains(pa1.id)) {
//                return pa1;
//            }
//            pa2 = ancestorHelper(tempC2);
//            anSet2.add(pa2.id);
//            tempC2 = pa2;
//            if (anSet1.contains(pa2.id)) {
//                return pa2;
//            }
//        }
//        return committed.get(0);
//    }
//
//    /* Helper function, find the latest ancestor (split point) and return its SHA-1. */
//    private Commit ancestorHelper(Commit c) {
//        Commit pa = null;
//        Commit tempC = c;
//        while (tempC.parent != null && tempC.parent.sibling == null) {
//            tempC = tempC.parent;
//            if (tempC.sibling != null) {
//                pa = tempC.parent;
//                break;
//            }
//        }
//        return pa;
//    }

    public boolean isAncestor(Commit c, Commit a) {
        Commit tempC = c;
        while (tempC.parent != null) {
            if (tempC.parent.id.equals(a.id)) {
                return true;
            }
            tempC = tempC.parent;
        }
        return false;
    }
}
