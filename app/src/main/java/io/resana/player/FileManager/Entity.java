package io.resana.player.FileManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.Comparator;

public class Entity implements Serializable {
    public Dir parent;
    public String name;
    private String path;
    public Type type;

    enum Type {
        Dir, Media
    }

    public Entity(Dir parent, String name, Type type) {
        this.parent = parent;
        this.name = name;
        this.type = type;
    }

    public String getPath() {
        if (path == null)
            createPath();
        return path;
    }

    public boolean isRoot() {
        return parent == null;
    }

    private void createPath() {
        if (parent != null)
            path = parent.getPath() + "/" + name;
        else
            path = name;
    }

    /* package */ static FilenameFilter mediaFilter;

    /* package */
    static FilenameFilter getFileFilter() {
        if (mediaFilter == null)
            mediaFilter = new FilenameFilter() {
                File f;

                public boolean accept(File dir, String name) {
                    if (name.endsWith(".mp4") || name.endsWith(".MP4")) {
                        return true;
                    }
                    f = new File(dir.getAbsolutePath() + "/" + name);

                    return f.isDirectory();
                }
            };
        return mediaFilter;
    }

    private static Comparator<Entity> comparator;

    /* package */
    static Comparator<Entity> getFileComparator() {
        if (comparator == null)
            comparator = new Comparator<Entity>() {
                @Override
                public int compare(Entity lhs, Entity rhs) {
                    return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                }
            };
        return comparator;
    }
}