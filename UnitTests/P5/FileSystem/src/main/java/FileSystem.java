public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);

        FileTableEntry dirEnt = open("/", "r");
        if (dirEnt != null) {
            int dirSize = fsize(dirEnt);
            if (dirSize > 0) {
                byte[] dirData = new byte[dirSize];
                read(dirEnt, dirData);
                directory.bytes2directory(dirData);
            }
            close(dirEnt);
        }
    }

    void sync() {
        FileTableEntry dirEnt = open("/", "w");
        if (dirEnt != null) {
            byte[] data = directory.directory2bytes();
            write(dirEnt, data);
            close(dirEnt);
        }
        superblock.sync();
    }

    boolean format(int files) {
        if (!filetable.fempty()) {
            return false;
        }

        superblock.format(files);
        superblock.freeList = 13;
        superblock.sync();

        directory = new Directory(files);
        filetable = new FileTable(directory);

        return true;
    }

    FileTableEntry open(String filename, String mode) {
        if (filename == null || mode == null) {
            return null;
        }

        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if (ftEnt == null) {
            return null;
        }

        if (mode.equals("w")) {
            if (!deallocAllBlocks(ftEnt)) {
                close(ftEnt);
                return null;
            }
            ftEnt.seekPtr = 0;
        }

        return ftEnt;
    }

    boolean close(FileTableEntry ftEnt) {
        if (ftEnt == null) {
            throw new NullPointerException();
        }

        synchronized (ftEnt) {
            if (ftEnt.count > 0) {
                ftEnt.count--;
            }
            if (ftEnt.count > 0) {
                return true;
            }
        }

        return filetable.ffree(ftEnt);
    }

    int fsize(FileTableEntry ftEnt) {
        if (ftEnt == null) {
            throw new NullPointerException();
        }

        synchronized (ftEnt) {
            return ftEnt.inode.length;
        }
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt == null || buffer == null) {
            throw new NullPointerException();
        }

        if (ftEnt.mode.equals("w") || ftEnt.mode.equals("a")) {
            return -1;
        }

        int bytesRead = 0;

        synchronized (ftEnt) {
            while (bytesRead < buffer.length && ftEnt.seekPtr < ftEnt.inode.length) {
                int targetBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (targetBlock < 0) {
                    break;
                }

                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(targetBlock, data);

                int blockOffset = ftEnt.seekPtr % Disk.blockSize;
                int bytesLeftInBlock = Disk.blockSize - blockOffset;
                int bytesLeftInFile = ftEnt.inode.length - ftEnt.seekPtr;
                int bytesToRead = Math.min(buffer.length - bytesRead,
                        Math.min(bytesLeftInBlock, bytesLeftInFile));

                System.arraycopy(data, blockOffset, buffer, bytesRead, bytesToRead);

                ftEnt.seekPtr += bytesToRead;
                bytesRead += bytesToRead;
            }
        }

        return bytesRead;
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt == null || buffer == null) {
            throw new NullPointerException();
        }

        if (ftEnt.mode.equals("r")) {
            return -1;
        }

        int bytesWritten = 0;

        synchronized (ftEnt) {
            while (bytesWritten < buffer.length) {
                int targetBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);

                if (targetBlock < 0) {
                    short freeBlock = (short) superblock.getFreeBlock();
                    if (freeBlock < 0) {
                        break;
                    }

                    short result = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeBlock);

                    if (result == Inode.ErrorIndirectNull) {
                        short indexBlock = (short) superblock.getFreeBlock();
                        if (indexBlock < 0) {
                            break;
                        }
                        if (!ftEnt.inode.registerIndexBlock(indexBlock)) {
                            break;
                        }
                        result = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeBlock);
                    }

                    if (result < 0) {
                        break;
                    }

                    targetBlock = freeBlock;
                }

                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(targetBlock, data);

                int blockOffset = ftEnt.seekPtr % Disk.blockSize;
                int bytesLeftInBlock = Disk.blockSize - blockOffset;
                int bytesToWrite = Math.min(bytesLeftInBlock, buffer.length - bytesWritten);

                System.arraycopy(buffer, bytesWritten, data, blockOffset, bytesToWrite);
                SysLib.rawwrite(targetBlock, data);

                ftEnt.seekPtr += bytesToWrite;
                bytesWritten += bytesToWrite;

                if (ftEnt.seekPtr > ftEnt.inode.length) {
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }

            ftEnt.inode.toDisk(ftEnt.iNumber);
        }

        return bytesWritten;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if (ftEnt == null || ftEnt.inode.count != 1) {
            return false;
        }

        for (int i = 0; i < Inode.directSize; i++) {
            if (ftEnt.inode.direct[i] >= 0) {
                superblock.returnBlock(ftEnt.inode.direct[i]);
                ftEnt.inode.direct[i] = Inode.UNASSIGNED;
            }
        }

        byte[] indirectData = ftEnt.inode.unregisterIndexBlock();
        if (indirectData != null) {
            for (int i = 0; i < Disk.blockSize / 2; i++) {
                short block = SysLib.bytes2short(indirectData, i * 2);
                if (block >= 0) {
                    superblock.returnBlock(block);
                }
            }
        }

        ftEnt.inode.length = 0;
        ftEnt.seekPtr = 0;
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    boolean delete(String filename) {
        if (filename == null) {
            return false;
        }

        FileTableEntry ftEnt = open(filename, "w");
        if (ftEnt == null) {
            return false;
        }

        short iNumber = ftEnt.iNumber;
        boolean closed = close(ftEnt);
        boolean freed = directory.ifree(iNumber);

        return closed && freed;
    }

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        if (ftEnt == null) {
            throw new NullPointerException();
        }

        synchronized (ftEnt) {
            switch (whence) {
                case 0:
                    ftEnt.seekPtr = offset;
                    break;
                case 1:
                    ftEnt.seekPtr += offset;
                    break;
                case 2:
                    ftEnt.seekPtr = ftEnt.inode.length + offset;
                    break;
                default:
                    return -1;
            }

            if (ftEnt.seekPtr < 0) {
                ftEnt.seekPtr = 0;
            }
            if (ftEnt.seekPtr > ftEnt.inode.length) {
                ftEnt.seekPtr = ftEnt.inode.length;
            }

            return ftEnt.seekPtr;
        }
    }
}