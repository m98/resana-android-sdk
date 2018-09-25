package io.resana.player.FileManager;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by hojjatimani on 5/22/2016 AD.
 */
public class Dir extends Entity {
    private static final String TAG = "Dir";
    public List<Dir> subDirs;
    public List<Media> medias;

    ContainsMediaState containsMedia;
    private List<Dir> cofigurationBasedSubDirs;
    private Dir configurationBasedParent;

    public boolean onlyMediaContainingSubDirs = FileManagerHelper.onlyMediaContainingSubDirs;
    public boolean collapseUnnecessaryIntermediateSubDirs = FileManagerHelper.collapseUnnecessaryIntermediateSubDirs;


    enum ContainsMediaState {
        YES, NO
    }



    public Dir(Dir parent, String name) {
        super(parent, name, Type.Dir);
        build();
    }

    private void build() {
        subDirs = new ArrayList<>();
        medias = new ArrayList<>();
        File f = new File(getPath());
        String[] list = f.list(getFileFilter());
        if (list != null) {
            for (String s : list) {
                if (new File(getPath(), s).isDirectory())
                    subDirs.add(new Dir(this, s));
                else
                    medias.add(new Media(this, s));
            }
        }
    }

    public void applyConfiguration() {
        List<Dir> temp;
        if (onlyMediaContainingSubDirs) {
            temp = subDirs;
            subDirs = new ArrayList<>();
            for (Dir oldSubdir : temp)
                if (oldSubdir.containsMedia())
                    subDirs.add(oldSubdir);

        }
        if (collapseUnnecessaryIntermediateSubDirs) {
            temp = subDirs;
            subDirs = new ArrayList<>();
            for (Dir oldSubdir : temp) {
                while (oldSubdir.isCollapsable())
                    oldSubdir = oldSubdir.subDirs.get(0);
                subDirs.add(oldSubdir);
            }
        }
        for (Dir subDir : subDirs) {
            subDir.parent = this;
            subDir.applyConfiguration();
        }
        Collections.sort(subDirs, getFileComparator());
        Collections.sort(medias, getFileComparator());
    }

    public void print(String offest) {
        for (Dir subDir : subDirs) {
            Log.d(TAG, offest + "-" + subDir.name);
            subDir.print(offest + "    ");
        }
        for (Media media : medias)
            Log.d(TAG, offest + media.name + "\n");
    }

//    public void printMediaContainers(String offset) {
//        for (Dir subDir : subDirs) {
//            if (subDir.containsMedia()) {
//                Log.d(TAG, offset + "-" + subDir.name);
//                subDir.printMediaContainers(offset + "    ");
//            }
//        }
//        for (Media media : medias)
//            Log.d(TAG, offset + media.name + "\n");
//    }

//    public List<Media> getMedias() {
//        return medias;
//    }

//    public List<Dir> getSubDirs() {
//        if (cofigurationBasedSubDirs == null)
//            createConfigurationBasedSubDirsList();
//        return cofigurationBasedSubDirs;
//    }
//
//    private void createConfigurationBasedSubDirsList() {
//        if (onlyMediaContainingSubDirs) {
//            cofigurationBasedSubDirs = new ArrayList<>();
//            for (Dir subDir : subDirs) {
//                if (subDir.containsMedia())
//                    cofigurationBasedSubDirs.add(subDir);
//            }
//        } else
//            cofigurationBasedSubDirs = subDirs;
//        if (collapseUnnecessaryIntermediateSubDirs) {
//            List<Dir> temp = cofigurationBasedSubDirs;
//            cofigurationBasedSubDirs = new ArrayList<>();
//            for (Dir subDir : temp) {
//                while (subDir.isCollapsable())
//                    subDir = subDir.getSubDirs().get(0);
//                cofigurationBasedSubDirs.add(subDir);
//            }
//        }
//    }

    public boolean isCollapsable() {
        return medias.size() == 0 && subDirs.size() == 1;
    }

    public boolean containsMedia() {
        if (containsMedia == null)
            checkIfDirecteryContainsMedia();
        return containsMedia == ContainsMediaState.YES;
    }

    public List<Media> getAllMediaRecursively() {
        List<Media> res = new ArrayList<Media>(medias);
        for (Dir subDir : subDirs) {
            res.addAll(subDir.getAllMediaRecursively());
        }
        return res;
    }

    private void checkIfDirecteryContainsMedia() {
        if (medias.size() > 0) {
            containsMedia = ContainsMediaState.YES;
            return;
        }
        for (Dir subDir : subDirs)
            if (subDir.containsMedia()) {
                containsMedia = ContainsMediaState.YES;
                return;
            }
        containsMedia = ContainsMediaState.NO;
    }
}