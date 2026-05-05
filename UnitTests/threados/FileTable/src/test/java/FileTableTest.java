import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FileTable class.
 * 
 * Tests the functionality of the FileTable implementation including:
 * - File table entry allocation and deallocation
 * - File access mode management (r, w, a, r+, w+, a+)
 * - Inode flag coordination and synchronization
 * - Thread-safe file access control
 * - Integration with Directory for filename resolution
 * 
 * Uses manual mock objects to test ONLY the FileTable logic 
 * without external dependencies.
 * 
 * @author Stephen Dame
 * @version 1.0
 * @since 2026
 */
public class FileTableTest {
    
    private TestDirectory testDirectory;
    private FileTable fileTable;
    
    @BeforeEach
    public void setUp() {
        testDirectory = new TestDirectory();
        fileTable = new FileTable(testDirectory);
    }
    
    @Test
    public void testConstructor() {
        // Verify that FileTable is properly initialized
        assertNotNull(fileTable);
        assertTrue(fileTable.fempty());
        assertNotNull(fileTable.getTable());
        assertEquals(0, fileTable.getTable().size());
    }
    
    @Test
    public void testFalloc_NewFile_WriteMode() {
        // Arrange
        String filename = "newfile.txt";
        String mode = "w";
        
        // File doesn't exist by default in our test directory
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(1, result.iNumber); // First allocated inode
        assertEquals(2, result.inode.flag); // Write mode flag
        assertEquals(1, result.inode.count); // Count incremented
        assertEquals(0, result.seekPtr); // Default seek position
        
        // Verify interactions
        assertTrue(testDirectory.nameiCalled);
        assertTrue(testDirectory.iallocCalled);
        assertEquals(filename, testDirectory.lastNameiFilename);
        assertEquals(filename, testDirectory.lastIallocFilename);
        
        // Verify file table state
        assertFalse(fileTable.fempty());
        assertEquals(1, fileTable.getTable().size());
        assertTrue(fileTable.getTable().contains(result));
    }
    
    @Test
    public void testFalloc_NewFile_AppendMode() {
        // Arrange
        String filename = "appendfile.txt";
        String mode = "a";
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(1, result.iNumber); // First allocated inode
        assertEquals(6, result.inode.flag); // Append mode flag
        assertEquals(1, result.inode.count);
        assertEquals(0, result.seekPtr); // New file, so length is 0
        
        assertTrue(testDirectory.nameiCalled);
        assertTrue(testDirectory.iallocCalled);
    }
    
    @Test
    public void testFalloc_ExistingFile_ReadMode() {
        // Arrange
        String filename = "existing.txt";
        String mode = "r";
        short existingINumber = 5;
        
        // Set up existing file
        testDirectory.setFileExists(filename, existingINumber);
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(existingINumber, result.iNumber);
        assertEquals(1, result.inode.flag); // Read mode flag
        assertEquals(1, result.inode.count);
        
        assertTrue(testDirectory.nameiCalled);
        assertFalse(testDirectory.iallocCalled); // Should not allocate new inode
    }
    
    @Test
    public void testFalloc_ExistingFile_WriteMode() {
        // Arrange
        String filename = "existing.txt";
        String mode = "w";
        short existingINumber = 7;
        
        testDirectory.setFileExists(filename, existingINumber);
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(existingINumber, result.iNumber);
        assertEquals(2, result.inode.flag); // Write mode flag
        assertEquals(1, result.inode.count);
        
        assertTrue(testDirectory.nameiCalled);
        assertFalse(testDirectory.iallocCalled);
    }
    
    @Test
    public void testFalloc_ExistingFile_AppendMode() {
        // Arrange
        String filename = "existing.txt";
        String mode = "a";
        short existingINumber = 9;
        
        testDirectory.setFileExists(filename, existingINumber);
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(existingINumber, result.iNumber);
        assertEquals(6, result.inode.flag); // Append mode flag
        assertEquals(1, result.inode.count);
        assertEquals(result.inode.length, result.seekPtr); // Seek to end for append
        
        assertTrue(testDirectory.nameiCalled);
        assertFalse(testDirectory.iallocCalled);
    }
    
    @Test
    public void testFalloc_RootDirectory() {
        // Arrange
        String rootPath = "/";
        String mode = "r";
        
        // Act
        FileTableEntry result = fileTable.falloc(rootPath, mode);
        
        // Assert
        assertNotNull(result);
        assertEquals(mode, result.mode);
        assertEquals(0, result.iNumber); // Root directory is always inode 0
        assertEquals(1, result.inode.flag); // Read mode flag
        
        // Should not call directory methods for root
        assertFalse(testDirectory.nameiCalled);
        assertFalse(testDirectory.iallocCalled);
    }
    
    @Test
    public void testFalloc_NonExistentFile_ReadMode_ReturnsNull() {
        // Arrange
        String filename = "nonexistent.txt";
        String mode = "r";
        
        // File doesn't exist by default
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, mode);
        
        // Assert
        assertNull(result);
        assertTrue(testDirectory.nameiCalled);
        assertFalse(testDirectory.iallocCalled);
        
        // File table should remain empty
        assertTrue(fileTable.fempty());
    }
    
    @Test
    public void testFalloc_InvalidMode_ReturnsNull() {
        // Arrange
        String filename = "testfile.txt";
        String invalidMode = "x";
        
        // Act
        FileTableEntry result = fileTable.falloc(filename, invalidMode);
        
        // Assert
        assertNull(result);
        
        // Should not allocate inode for invalid mode
        assertFalse(testDirectory.iallocCalled);
        assertTrue(fileTable.fempty());
    }
    
    @Test
    public void testFfree_ValidEntry() {
        // Arrange
        String filename = "testfile.txt";
        FileTableEntry entry = fileTable.falloc(filename, "w");
        assertNotNull(entry);
        assertFalse(fileTable.fempty());
        
        // Act
        boolean result = fileTable.ffree(entry);
        
        // Assert
        assertTrue(result);
        assertEquals(0, entry.inode.count); // Count decremented
        assertEquals(0, entry.inode.flag); // Flag reset to unused
        assertTrue(fileTable.fempty()); // Table should be empty
        assertEquals(0, fileTable.getTable().size());
    }
    
    @Test
    public void testFfree_InvalidEntry_ReturnsFalse() {
        // Arrange
        FileTableEntry fakeEntry = new FileTableEntry(new Inode(), (short) 99, "r");
        
        // Act
        boolean result = fileTable.ffree(fakeEntry);
        
        // Assert
        assertFalse(result);
        assertTrue(fileTable.fempty()); // Table should remain empty
    }
    
    @Test
    public void testFfree_AppendModeEntry() {
        // Arrange
        String filename = "appendfile.txt";
        FileTableEntry entry = fileTable.falloc(filename, "a");
        assertEquals(6, entry.inode.flag); // Verify append flag
        
        // Act
        boolean result = fileTable.ffree(entry);
        
        // Assert
        assertTrue(result);
        assertEquals(0, entry.inode.flag); // Append flag should be reset to 0
        assertEquals(0, entry.inode.count);
    }
    
    @Test
    public void testMultipleEntries() {
        // Arrange & Act
        FileTableEntry entry1 = fileTable.falloc("file1.txt", "w");
        FileTableEntry entry2 = fileTable.falloc("file2.txt", "w"); // Changed from "r" to "w"
        
        // Assert
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertFalse(fileTable.fempty());
        assertEquals(2, fileTable.getTable().size());
        
        // Clean up first entry
        assertTrue(fileTable.ffree(entry1));
        assertEquals(1, fileTable.getTable().size());
        assertFalse(fileTable.fempty());
        
        // Clean up second entry
        assertTrue(fileTable.ffree(entry2));
        assertEquals(0, fileTable.getTable().size());
        assertTrue(fileTable.fempty());
    }
    
    @Test
    public void testMultipleEntries_WriteAndRead() {
        // Arrange - Create a file first, then read it
        testDirectory.setFileExists("existing.txt", (short) 5);
        
        // Act
        FileTableEntry writeEntry = fileTable.falloc("newfile.txt", "w");
        FileTableEntry readEntry = fileTable.falloc("existing.txt", "r");
        
        // Assert
        assertNotNull(writeEntry);
        assertNotNull(readEntry);
        assertFalse(fileTable.fempty());
        assertEquals(2, fileTable.getTable().size());
        
        // Verify different modes and inode numbers
        assertEquals("w", writeEntry.mode);
        assertEquals("r", readEntry.mode);
        assertEquals(2, writeEntry.inode.flag); // Write flag
        assertEquals(1, readEntry.inode.flag);  // Read flag
        assertNotEquals(writeEntry.iNumber, readEntry.iNumber);
        
        // Clean up
        assertTrue(fileTable.ffree(writeEntry));
        assertTrue(fileTable.ffree(readEntry));
        assertTrue(fileTable.fempty());
    }
    
    @Test
    public void testGetTable() {
        // Arrange
        FileTableEntry entry = fileTable.falloc("test.txt", "w");
        
        // Act
        var table = fileTable.getTable();
        
        // Assert
        assertNotNull(table);
        assertEquals(1, table.size());
        assertTrue(table.contains(entry));
        
        // Verify it's the actual table (not a copy)
        assertSame(fileTable.getTable(), table);
    }
    
    @Test
    public void testFalloc_MultipleReaders_SameFile() {
        // Arrange
        String filename = "multiread.txt";
        testDirectory.setFileExists(filename, (short) 10);
        
        // Act - Open same file for reading multiple times
        FileTableEntry reader1 = fileTable.falloc(filename, "r");
        FileTableEntry reader2 = fileTable.falloc(filename, "r");
        
        // Assert
        assertNotNull(reader1);
        assertNotNull(reader2);
        assertEquals(1, reader1.inode.flag); // Should remain in read mode
        assertEquals(1, reader2.inode.flag); // Should remain in read mode
        
        // Both should point to the same inode number
        assertEquals(10, reader1.iNumber);
        assertEquals(10, reader2.iNumber);
        
        // cleanup
        fileTable.ffree(reader1);
        fileTable.ffree(reader2);
    }
    
    /**
     * Test-specific Directory implementation that tracks method calls
     * and provides controllable behavior for testing.
     */
    private static class TestDirectory extends Directory {
        
        boolean nameiCalled = false;
        boolean iallocCalled = false;
        String lastNameiFilename = null;
        String lastIallocFilename = null;
        
        // Test helper: Map of filename to inode number for simulating existing files
        private Map<String, Short> existingFiles = new HashMap<>();
        
        public TestDirectory() {
            super(100); // Call parent constructor
        }
        
        /**
         * Test helper method to simulate an existing file in the directory.
         * When namei() is called for this filename, it will return the specified inode number.
         * 
         * @param filename The filename to simulate
         * @param inodeNumber The inode number to return for this filename
         */
        public void setFileExists(String filename, short inodeNumber) {
            existingFiles.put(filename, inodeNumber);
        }
        
        @Override
        public short namei(String filename) {
            nameiCalled = true;
            lastNameiFilename = filename;
            
            // Check if this is a simulated existing file
            if (existingFiles.containsKey(filename)) {
                return existingFiles.get(filename);
            }
            
            return super.namei(filename); // Use parent's implementation
        }
        
        @Override
        public short ialloc(String filename) {
            iallocCalled = true;
            lastIallocFilename = filename;
            return super.ialloc(filename); // Use parent's implementation
        }
        
        public void reset() {
            nameiCalled = false;
            iallocCalled = false;
            lastNameiFilename = null;
            lastIallocFilename = null;
            existingFiles.clear(); // Clear simulated existing files
        }
    }
}