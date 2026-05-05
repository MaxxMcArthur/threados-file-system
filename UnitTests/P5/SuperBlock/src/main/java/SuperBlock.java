public class SuperBlock {
    private final int defaultInodeBlocks = Constants.DEFAULT_INODE_BLOCKS;

    public int totalBlocks;
    public int inodeBlocks;
    public int freeList;

    public SuperBlock(int diskBlocks) {
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(Constants.SUPERBLOCK_BLOCK, data);

        totalBlocks = SysLib.bytes2int(data, 0);
        inodeBlocks = SysLib.bytes2int(data, 4);
        freeList = SysLib.bytes2int(data, 8);

        if (totalBlocks == diskBlocks && inodeBlocks > 0 && freeList >= 0) {
            return;
        }

        totalBlocks = diskBlocks;
        format(defaultInodeBlocks);
    }

    void sync() {
        byte[] data = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, data, 0);
        SysLib.int2bytes(inodeBlocks, data, 4);
        SysLib.int2bytes(freeList, data, 8);
        SysLib.rawwrite(Constants.SUPERBLOCK_BLOCK, data);
    }

    void format() {
        format(defaultInodeBlocks);
    }

    void format(int inodeBlocks) {
        this.inodeBlocks = inodeBlocks;

        for (short i = 0; i < inodeBlocks; i++) {
            Inode inode = new Inode();
            inode.flag = 0;
            inode.toDisk(i);
        }

        freeList = 1 + (inodeBlocks * Inode.iNodeSize) / Disk.blockSize;

        for (int i = freeList; i < totalBlocks; i++) {
            byte[] data = new byte[Disk.blockSize];
            int next = (i + 1 < totalBlocks) ? i + 1 : 0;
            SysLib.int2bytes(next, data, 0);
            SysLib.rawwrite(i, data);
        }

        sync();
    }

    public int getFreeBlock() {
        if (freeList <= 0) return -1;

        int allocated = freeList;
        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(allocated, data);
        freeList = SysLib.bytes2int(data, 0);

        byte[] clear = new byte[Disk.blockSize];
        SysLib.rawwrite(allocated, clear);

        return allocated;
    }

    public boolean returnBlock(int oldBlockNumber) {
        if (oldBlockNumber < 0) return false;

        byte[] data = new byte[Disk.blockSize];
        SysLib.int2bytes(freeList, data, 0);
        SysLib.rawwrite(oldBlockNumber, data);
        freeList = oldBlockNumber;
        return true;
    }
}