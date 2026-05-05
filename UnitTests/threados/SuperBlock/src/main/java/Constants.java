/**
 * The Constants class defines system-wide constants used throughout the file system
 * implementation. These constants control various aspects of the file system
 * including block sizes, limits, and default values.
 * 
 * This class contains constants for:
 * - File system structure parameters (inode blocks, block sizes)
 * - File and directory limits (filename length, maximum inodes)
 * - Test configuration values
 * - Block and inode identifiers for testing
 * 
 * @author Stephen Dame
 * @since 2026
 */
public class Constants {
    /**
     * Default number of inode blocks to allocate when formatting the file system.
     */
    public static final int DEFAULT_INODE_BLOCKS = 64;
    
    /**
     * Block number where the superblock is stored (always block 0).
     */
    public static final int SUPERBLOCK_BLOCK = 0;
    
    /**
     * Size of each inode structure in bytes.
     */
    public static final int INODE_SIZE = 32;
    
    /**
     * Number of direct block pointers in each inode.
     */
    public static final int DIRECT_SIZE = 11;
    
    /**
     * Maximum length allowed for file names.
     */
    public static final int MAX_FILENAME_LENGTH = 30;
    
    /**
     * Maximum number of inodes supported by the file system.
     */
    public static final int MAX_INODES = 64;
    
    /**
     * Number of inode blocks used in testing scenarios.
     */
    public static final int TEST_INODE_BLOCKS = 64;
    
    /**
     * Starting block number for the free list in test scenarios.
     */
    public static final int TEST_FREE_LIST_START = 13;
    
    /**
     * Next free list starting block number for testing.
     */
    public static final int TEST_NEXT_FREE_LIST_START = 13;
    
    /**
     * Block number used for testing block return operations.
     */
    public static final int TEST_OLD_BLOCK_NUMBER = 50;
    
    /**
     * Total number of disk blocks used in test scenarios.
     */
     public static final int TEST_DISK_BLOCKS = 1000;
     
     /**
      * Primary inode number used in testing.
      */
     public static final short INODE_NUMBER = 5;
     
     /**
      * Secondary inode number used in testing.
      */
     public static final short INODE_NUMBER_2 = 6;
     
     /**
      * Primary indirect block number for testing.
      */
     public static final short INDIRECT_BLOCK_NUMBER = 10;
     
     /**
      * Secondary indirect block number for testing.
      */
     public static final short INDIRECT_BLOCK_NUMBER_2 = 15;
     
     /**
      * Tertiary indirect block number for testing.
      */
     public static final short INDIRECT_BLOCK_NUMBER_3 = 30;
     
     /**
      * Target block number for indirect block testing.
      */
     public static final short INDIRECT_TARGET_BLOCK = 40;
     
     /**
      * Indirect block number used for registration testing.
      */
     public static final short INDIRECT_BLOCK_NUMBER_REGISTER = 50;
     
     /**
      * Target block number for indirect block registration testing.
      */
     public static final short INDIRECT_TARGET_BLOCK_REGISTER = 60;

     /**
      * Name of an existing file used in testing scenarios.
      */
     public static final String EXISTING_FILE = "existingfile";
     
     /**
      * Generic test file name used in various test scenarios.
      */
     public static final String TEST_FILENAME = "testfile";
}