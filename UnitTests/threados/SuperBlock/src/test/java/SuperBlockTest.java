import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Unit tests for the SuperBlock class.
 * 
 * Tests the functionality of the SuperBlock implementation including:
 * - Superblock creation and initialization
 * - Disk formatting operations
 * - Free block management
 * - Inode allocation and deallocation
 * - Superblock persistence and recovery
 * 
 * @author Stephen Dame
 * @version 1.0
 * @since 2026
 */
public class SuperBlockTest {

    private SuperBlock superBlock;

    @BeforeEach
    public void setUp() throws IOException {
        SysLib.initDisk(); // Reset the disk before each test
        superBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
    }

    @Test
    void testSync() {
        // Act
        superBlock.sync(); // This now writes to the real disk
        // Assert
        SuperBlock reloadedSuperBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        assertEquals(superBlock.totalBlocks, reloadedSuperBlock.totalBlocks);
        assertEquals(superBlock.inodeBlocks, reloadedSuperBlock.inodeBlocks);
        assertEquals(superBlock.freeList, reloadedSuperBlock.freeList);
    }

    @Test
    void testDefaultConstructor() {
        // Arrange
        int diskSize = Constants.TEST_DISK_BLOCKS;
        int defaultInodeBlocks = 64;

        // Act
        SuperBlock defaultSuperBlock = new SuperBlock(diskSize);

        // Assert
        assertEquals(diskSize, defaultSuperBlock.totalBlocks);
        assertEquals(defaultInodeBlocks, defaultSuperBlock.inodeBlocks);
        assertEquals(1 + (defaultInodeBlocks * Inode.iNodeSize) / Disk.blockSize, defaultSuperBlock.freeList);
    }

    @Test
    void testFormat() {
        // Arrange
        int diskBlocks = Disk.blockSize;
        SuperBlock sb = new SuperBlock(Constants.DEFAULT_INODE_BLOCKS);
        // modify the super block
        sb.totalBlocks = Disk.blockSize;
        sb.inodeBlocks = Constants.DEFAULT_INODE_BLOCKS;
        sb.freeList = 200;

        // Act
        sb.format(Constants.DEFAULT_INODE_BLOCKS);
        sb.sync();
        SuperBlock reloadedSuperBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        // Assert
        assertEquals(Constants.TEST_DISK_BLOCKS, reloadedSuperBlock.totalBlocks);
        assertEquals(Constants.DEFAULT_INODE_BLOCKS, reloadedSuperBlock.inodeBlocks);
        assertEquals(1 + (Constants.DEFAULT_INODE_BLOCKS * Inode.iNodeSize) / Disk.blockSize, reloadedSuperBlock.freeList);
    }

    @Test
    void testDefaultFormat() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int originalFreeList = sb.freeList;
        
        // Act
        sb.format(); // Uses default inode blocks
        
        // Assert
        assertEquals(Constants.DEFAULT_INODE_BLOCKS, sb.inodeBlocks);
        assertEquals(1 + (Constants.DEFAULT_INODE_BLOCKS * Inode.iNodeSize) / Disk.blockSize, sb.freeList);
        
        // Verify format was applied by checking disk
        SuperBlock reloadedSuperBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        assertEquals(Constants.DEFAULT_INODE_BLOCKS, reloadedSuperBlock.inodeBlocks);
    }

    @Test
    void testGetFreeBlock_SingleBlock() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int originalFreeList = sb.freeList;
        
        // Act
        int freeBlock = sb.getFreeBlock();
        
        // Assert
        assertEquals(originalFreeList, freeBlock);
        assertNotEquals(originalFreeList, sb.freeList); // Free list should have moved
        assertTrue(sb.freeList > originalFreeList || sb.freeList == 0); // Next block or end of list
    }

    @Test
    void testGetFreeBlock_MultipleBlocks() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int firstFreeBlock = sb.freeList;
        
        // Act
        int block1 = sb.getFreeBlock();
        int block2 = sb.getFreeBlock();
        int block3 = sb.getFreeBlock();
        
        // Assert
        assertEquals(firstFreeBlock, block1);
        assertNotEquals(block1, block2);
        assertNotEquals(block2, block3);
        assertNotEquals(block1, block3);
        
        // Blocks should be sequential (linked list behavior)
        assertEquals(block1 + 1, block2);
        assertEquals(block2 + 1, block3);
    }

    @Test
    void testReturnBlock_ValidBlock() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int originalFreeList = sb.freeList;
        int blockToReturn = 100; // Some valid block number
        
        // Act
        boolean result = sb.returnBlock(blockToReturn);
        
        // Assert
        assertTrue(result);
        assertEquals(blockToReturn, sb.freeList); // Returned block becomes new head of free list
        
        // Verify the returned block points to the original free list
        byte[] blockData = new byte[Disk.blockSize];
        SysLib.rawread(blockToReturn, blockData);
        int nextFreeBlock = SysLib.bytes2int(blockData, 0);
        assertEquals(originalFreeList, nextFreeBlock);
    }

    @Test
    void testReturnBlock_InvalidBlock() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int originalFreeList = sb.freeList;
        
        // Act & Assert
        assertFalse(sb.returnBlock(-1)); // Negative block number
        assertFalse(sb.returnBlock(-100)); // Another negative block number
        
        // Free list should remain unchanged
        assertEquals(originalFreeList, sb.freeList);
    }

    @Test
    void testFreeBlockLifecycle() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        
        // Act - Get a block, then return it
        int allocatedBlock = sb.getFreeBlock();
        int freeListAfterAlloc = sb.freeList;
        boolean returnResult = sb.returnBlock(allocatedBlock);
        
        // Assert
        assertTrue(returnResult);
        assertEquals(allocatedBlock, sb.freeList); // Returned block is now head of free list
        
        // Get the same block again - should be the returned block
        int reallocatedBlock = sb.getFreeBlock();
        assertEquals(allocatedBlock, reallocatedBlock);
        assertEquals(freeListAfterAlloc, sb.freeList); // Free list restored to previous state
    }

    @Test
    void testConstructor_ValidSuperBlockOnDisk() throws IOException {
        // Arrange - Create a valid superblock on disk first
        SysLib.initDisk();
        SuperBlock initialSB = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        initialSB.sync(); // Write to disk
        
        // Act - Create new superblock (should read from disk)
        SuperBlock loadedSB = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        
        // Assert - Should match the initial superblock
        assertEquals(initialSB.totalBlocks, loadedSB.totalBlocks);
        assertEquals(initialSB.inodeBlocks, loadedSB.inodeBlocks);
        assertEquals(initialSB.freeList, loadedSB.freeList);
    }

    @Test
    void testConstructor_InvalidSuperBlockOnDisk() throws IOException {
        // Arrange - Write invalid data to superblock location
        SysLib.initDisk();
        byte[] invalidData = new byte[Disk.blockSize];
        SysLib.int2bytes(-1, invalidData, 0); // Invalid totalBlocks
        SysLib.int2bytes(0, invalidData, 4);  // Invalid inodeBlocks
        SysLib.int2bytes(-1, invalidData, 8); // Invalid freeList
        SysLib.rawwrite(Constants.SUPERBLOCK_BLOCK, invalidData);
        
        // Act - Create superblock (should format due to invalid data)
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        
        // Assert - Should have formatted with default values
        assertEquals(Constants.TEST_DISK_BLOCKS, sb.totalBlocks);
        assertEquals(Constants.DEFAULT_INODE_BLOCKS, sb.inodeBlocks);
        assertEquals(1 + (Constants.DEFAULT_INODE_BLOCKS * Inode.iNodeSize) / Disk.blockSize, sb.freeList);
    }

    @Test
    void testFreeListLinking() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        int startingFreeList = sb.freeList;
        
        // Act - Get several blocks to test the linked list structure
        int block1 = sb.getFreeBlock();
        int block2 = sb.getFreeBlock();
        int block3 = sb.getFreeBlock();
        
        // Assert - Verify the free list was properly linked
        assertEquals(startingFreeList, block1);
        assertEquals(startingFreeList + 1, block2);
        assertEquals(startingFreeList + 2, block3);
        assertEquals(startingFreeList + 3, sb.freeList);
    }

    @Test
    void testFormatInitializesInodes() throws IOException {
        // Arrange - Create a completely isolated test environment
        SysLib.initDisk();
        SysLib.clearCache();
        
        // Create a SuperBlock and format it
        SuperBlock isolatedSB = new SuperBlock(300); // Use unique size
        
        // First, let's corrupt some inodes to ensure format actually resets them
        Inode corruptedInode = new Inode();
        corruptedInode.flag = 99;
        corruptedInode.length = 12345;
        corruptedInode.count = 67;
        corruptedInode.toDisk((short) 5); // Corrupt inode 5
        
        // Act - Format should reset all inodes
        isolatedSB.format(10);
        
        // Assert - Check that the corrupted inode was reset
        Inode resetInode = new Inode((short) 5);
        assertEquals(0, resetInode.flag, "Format should reset inode flag to 0");
        assertEquals(0, resetInode.length, "Format should reset inode length to 0");
        assertEquals(0, resetInode.count, "Format should reset inode count to 0");
        
        // Also check a few other inodes to ensure they're properly initialized
        for (short i = 6; i < 10; i++) {
            Inode inode = new Inode(i);
            assertEquals(0, inode.flag, "Inode " + i + " should be initialized with flag=0");
            assertEquals(0, inode.length, "Inode " + i + " should be initialized with length=0");
            assertEquals(0, inode.count, "Inode " + i + " should be initialized with count=0");
        }
        
        // Verify superblock fields were set correctly
        assertEquals(10, isolatedSB.inodeBlocks);
        assertEquals(1 + (10 * Inode.iNodeSize) / Disk.blockSize, isolatedSB.freeList);
    }

    @Test
    void testSyncPersistence() {
        // Arrange
        SuperBlock sb = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        sb.totalBlocks = 999;
        sb.inodeBlocks = 50;
        sb.freeList = 100;
        
        // Act
        sb.sync();
        
        // Assert - Create new superblock and verify data persisted
        SuperBlock loadedSB = new SuperBlock(999); // Use same totalBlocks to avoid reformatting
        assertEquals(999, loadedSB.totalBlocks);
        assertEquals(50, loadedSB.inodeBlocks);
        assertEquals(100, loadedSB.freeList);
    }
}