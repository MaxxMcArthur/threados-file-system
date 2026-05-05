import java.util.Vector;

public class FileTable {
    private Vector<FileTableEntry> table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector<FileTableEntry>();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fname, String mode) {
        if (fname == null || mode == null) return null;

        if (!(mode.equals("r") || mode.equals("w") || mode.equals("w+") || mode.equals("a"))) {
            return null;
        }

        short iNumber;
        Inode inode;

        if (fname.equals("/")) {
            iNumber = 0;
            inode = new Inode(iNumber);
        } else {
            iNumber = dir.namei(fname);

            if (iNumber >= 0) {
                inode = new Inode(iNumber);
            } else {
                if (mode.equals("r")) {
                    return null;
                }

                iNumber = dir.ialloc(fname);
                if (iNumber < 0) return null;

                inode = new Inode();   // important for new file tests
            }
        }

        if (mode.equals("r")) {
            inode.flag = 1;
        } else if (mode.equals("a")) {
            inode.flag = 6;
        } else {
            inode.flag = 2;
        }

        inode.count++;
        inode.toDisk(iNumber);

        FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);

        if (mode.equals("a")) {
            entry.seekPtr = inode.length;
        } else {
            entry.seekPtr = 0;
        }

        table.add(entry);
        return entry;
    }

    public synchronized boolean ffree(FileTableEntry e) {
        if (e == null) return false;
        if (!table.remove(e)) return false;

        if (e.inode.count > 0) {
            e.inode.count--;
        }

        if (e.inode.count == 0) {
            e.inode.flag = 0;
        }

        e.inode.toDisk(e.iNumber);
        notifyAll();
        return true;
    }

    public synchronized boolean fempty() {
        return table.isEmpty();
    }

    public Vector<FileTableEntry> getTable() {
        return table;
    }
}