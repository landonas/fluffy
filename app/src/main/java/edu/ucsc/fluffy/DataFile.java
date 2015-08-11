package edu.ucsc.fluffy;

import java.io.File;

/**
 * Created by mrg on 7/6/15.
 */

public class DataFile {

    File f = null;
    boolean selected = false;

    public DataFile(File f, boolean selected) {
        super();
        this.f = f;
        this.selected = selected;
    }

    public String getName() {
        return f.getName();
    }
    public File getFile() {
        return f;
    }
    public void setName(String name) {
        this.f = new File(name);
    }

    public boolean isSelected() {
        return selected;
    }
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
