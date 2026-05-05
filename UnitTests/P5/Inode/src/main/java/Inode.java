public class Inode {
    public final static int iNodeSize = Constants.INODE_SIZE;
    public final static int directSize = Constants.DIRECT_SIZE;
    public final static int iNodesPerBlock = Disk.blockSize / iNodeSize;

    public final static short NoError = 0;
    public final static short ErrorBlockRegistered = -1;
    public final static short ErrorPrecBlockUnused = -2;
    public final static short ErrorIndirectNull = -3;
    public final static short UNASSIGNED = -1;

    public int length;
    public short count;
    public short flag;
    public short[] direct = new short[directSize];
    public short indirect;

    public Inode() {
        length = 0;
        count = 0;
        flag = 0;
        for (int i = 0; i < directSize; i++) {
            direct[i] = UNASSIGNED;
        }
        indirect = UNASSIGNED;
    }

    public Inode(short iNumber) {
        this();

        int blockNumber = 1 + iNumber / iNodesPerBlock;
        int offset = (iNumber % iNodesPerBlock) * iNodeSize;

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);

        length = SysLib.bytes2int(data, offset);
        offset += 4;
        count = SysLib.bytes2short(data, offset);
        offset += 2;
        flag = SysLib.bytes2short(data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset);
            offset += 2;
        }

        indirect = SysLib.bytes2short(data, offset);
    }

    public void toDisk(short iNumber) {
        int blockNumber = 1 + iNumber / iNodesPerBlock;
        int offset = (iNumber % iNodesPerBlock) * iNodeSize;

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, data);

        SysLib.int2bytes(length, data, offset);
        offset += 4;
        SysLib.short2bytes(count, data, offset);
        offset += 2;
        SysLib.short2bytes(flag, data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], data, offset);
            offset += 2;
        }

        SysLib.short2bytes(indirect, data, offset);
        SysLib.rawwrite(blockNumber, data);
    }

    public int findTargetBlock(int offset) {
        int blockIndex = offset / Disk.blockSize;

        if (blockIndex < directSize) {
            return direct[blockIndex];
        }

        if (indirect == UNASSIGNED) {
            return UNASSIGNED;
        }

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(indirect, data);

        int indirectIndex = blockIndex - directSize;
        return SysLib.bytes2short(data, indirectIndex * 2);
    }

    public int findIndexBlock() {
        return indirect;
    }

    public short registerTargetBlock(int offset, short targetBlock) {
        int blockIndex = offset / Disk.blockSize;

        if (blockIndex < directSize) {
            if (direct[blockIndex] != UNASSIGNED) {
                return ErrorBlockRegistered;
            }
            if (blockIndex > 0 && direct[blockIndex - 1] == UNASSIGNED) {
                return ErrorPrecBlockUnused;
            }

            direct[blockIndex] = targetBlock;
            return NoError;
        }

        if (indirect == UNASSIGNED) {
            return ErrorIndirectNull;
        }

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(indirect, data);

        int indirectIndex = blockIndex - directSize;
        short existing = SysLib.bytes2short(data, indirectIndex * 2);
        if (existing != UNASSIGNED) {
            return ErrorBlockRegistered;
        }

        SysLib.short2bytes(targetBlock, data, indirectIndex * 2);
        SysLib.rawwrite(indirect, data);
        return NoError;
    }

    public boolean registerIndexBlock(short indexBlockNumber) {
        for (int i = 0; i < directSize; i++) {
            if (direct[i] == UNASSIGNED) {
                return false;
            }
        }

        if (indirect != UNASSIGNED) {
            return false;
        }

        indirect = indexBlockNumber;

        byte[] data = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize / 2; i++) {
            SysLib.short2bytes(UNASSIGNED, data, i * 2);
        }
        SysLib.rawwrite(indirect, data);
        return true;
    }

    public byte[] unregisterIndexBlock() {
        if (indirect == UNASSIGNED) {
            return null;
        }

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(indirect, data);

        byte[] cleared = new byte[Disk.blockSize];
        SysLib.rawwrite(indirect, cleared);

        indirect = UNASSIGNED;
        return data;
    }

    public void printInode() {
        System.err.println("Inode Debug Information:");
        System.err.println("  Length: " + length);
        System.err.println("  Count: " + count);
        System.err.println("  Flag: " + flag);
        System.err.println("  Indirect Pointer: " + indirect);
        for (int i = 0; i < directSize; i++) {
            System.err.println("    direct[" + i + "]: " + direct[i]);
        }
        System.err.println("End Inode Debug Information.");
    }
}