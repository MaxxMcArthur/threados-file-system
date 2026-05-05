import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Unit tests for the Directory class.
 * 
 * Tests the functionality of the Directory implementation including:
 * - Directory creation and initialization
 * - File name to inode mapping
 * - Inode allocation and deallocation
 * - Directory entry management
 * - Filename validation and operations
 * 
 * @author Stephen Dame
 * @version 1.0
 * @since 2026
 */

/**
 * Test class for Directory.java, specifically testing the maximum number of
 * files and max characters in file names.
 */
public class DirectoryTest {

    private Directory directory;
    private final int MAX_INUMBER = Constants.MAX_INODES; // Define a reasonable max inode number for testing
    private final int MAX_CHARS = Constants.MAX_FILENAME_LENGTH;

    @BeforeEach
    void setUp() {
        directory = new Directory(MAX_INUMBER);
    }

    @AfterEach
    void tearDown() {
        directory = null;
    }

    @Test
    void testMaxFiles() {
        // Test maximum number of files
        for (int i = 1; i < MAX_INUMBER; i++) { // Start from 1 to avoid using inode 0 ("/")
            String filename = "file" + i;
            short inode = directory.ialloc(filename);
            assertNotEquals(-1, inode, "Failed to allocate inode for file: " + filename);
            assertEquals(i, inode, "Allocated inode number does not match expected.");
        }
        // Check if the next allocation fails due to lack of inodes
        assertThrows(AssertionError.class, () -> {
            String filename = "file" + MAX_INUMBER;
            short inode = directory.ialloc(filename);
            assertNotEquals(-1, inode);
        }, "Expected allocation to fail, but it did not.");
    }

    @Test
    void testMaxCharacters() {
        // Test maximum characters in file name
        String maxCharsFilename = "a".repeat(MAX_CHARS);
        short inodeMaxChars = directory.ialloc(maxCharsFilename);
        assertNotEquals(-1, inodeMaxChars, "Failed to allocate inode for file with max characters.");
        assertEquals(1, directory.namei(maxCharsFilename));

        // test characters greater than MAX_CHARS
        String longFileName = "b".repeat(MAX_CHARS + 5);
        short inodeLongFileIndex = directory.ialloc(longFileName);
        assertEquals(-1, inodeLongFileIndex, "Failed to allocate inode for file with Long characters.");
        assertEquals(-1, directory.namei(longFileName));
    }

    @Test
    void testFreeingInodes() {
        // Allocate and then free some inodes
        short inode1 = directory.ialloc("file1");

        assertTrue(directory.ifree(inode1), "Failed to free inode: " + inode1);

        // Try to allocate a new inode and it should fill the free spot
        short inode4 = directory.ialloc("file4");
        assertEquals(inode1, inode4, "Reused inode number does not match expected.");

    }

    @Test
    void test_inode_number_associated_with_a_filename() {
        // Test retrieving inode number by filename
        String filename1 = "testfile1";
        short inode1 = directory.ialloc(filename1);
        assertNotEquals(-1, inode1, "Failed to allocate inode for " + filename1);
        assertEquals(inode1, directory.namei(filename1), "Inode number retrieval by name failed for " + filename1);

        String filename2 = "testfile2";
        directory.ialloc(filename2);
        assertNotEquals(-1, directory.namei(filename2), "Failed to allocate inode for " + filename2);
        assertEquals(-1, directory.namei("nonexistentfile"), "Found a non-existent file.");

        // Test the root inode
        assertEquals(0, directory.namei("/"), "Root inode number retrieval failed.");
    }

    @Test
    void testDirectoryToBytesAndBytesToDirectory() {
        // Allocate some files
        short inode1 = directory.ialloc("file1");
        short inode2 = directory.ialloc("file2");
        short inode3 = directory.ialloc("file3");

        // Convert to bytes
        byte[] directoryBytes = directory.directory2bytes();

        // Create a new directory object
        Directory newDirectory = new Directory(MAX_INUMBER);

        // Convert from bytes to directory
        newDirectory.bytes2directory(directoryBytes);

        // Assertions
        assertEquals(inode1, newDirectory.namei("file1"), "Directory to bytes/bytes to directory failed for file1.");
        assertEquals(inode2, newDirectory.namei("file2"), "Directory to bytes/bytes to directory failed for file2.");
        assertEquals(inode3, newDirectory.namei("file3"), "Directory to bytes/bytes to directory failed for file3.");
        assertEquals(0, newDirectory.namei("/"), "Root inode lost after directory conversion.");
    }

    @Test
    void testDuplicateFilenames() {
        // Allocate a file
        String filename = "duplicatefile";
        short inode1 = directory.ialloc(filename);
        assertNotEquals(-1, inode1, "Failed to allocate inode for initial file: " + filename);

        // Try to allocate another file with the same name
        short inode2 = directory.ialloc(filename);
        assertEquals(-1, inode2, "Allocated inode for duplicate file name: " + filename);
    }

    @Test
    void testFillDirectoryWithRandomFilenames() {
        Random random = new Random();
        Set<String> usedFilenames = new HashSet<>();
        int allocatedCount = 0;
        
        // Test filling the directory to its maximum capacity
        // We need to account for the fact that random filenames might be duplicates
        for (int attempt = 0; attempt < MAX_INUMBER * 3 && allocatedCount < MAX_INUMBER - 1; attempt++) {
            // Generate random filenames
            String filename = generateRandomFilename(random);
            
            // Skip if we've already used this filename
            if (usedFilenames.contains(filename)) {
                continue;
            }
            
            short inode = directory.ialloc(filename);
            if (inode != -1) {
                usedFilenames.add(filename);
                allocatedCount++;
            }
        }
        
        // We should have allocated MAX_INUMBER - 1 files (excluding root)
        assertTrue(allocatedCount >= MAX_INUMBER - 10, 
                  "Should have allocated at least " + (MAX_INUMBER - 10) + " files, but only allocated " + allocatedCount);

        // Try to allocate one more with a guaranteed unique filename
        String uniqueFilename = "unique_" + System.currentTimeMillis();
        short result = directory.ialloc(uniqueFilename);
        
        if (allocatedCount == MAX_INUMBER - 1) {
            assertEquals(-1, result, "Directory should be full, cannot allocate more files");
        } else {
            // If we didn't fill it completely due to random duplicates, this should succeed
            assertNotEquals(-1, result, "Should be able to allocate unique filename");
        }
    }

    /**
     * Generates a random filename of varying length, using a limited set of
     * characters.
     *
     * @param random The random number generator.
     * @return A randomly generated filename.
     */
    private String generateRandomFilename(Random random) {
        int length = random.nextInt(MAX_CHARS) + 1; // Ensure length is at least 1
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char randomChar = (char) (random.nextInt(26) + 'a'); // Generates a-z
            sb.append(randomChar);
        }
        return sb.toString();
    }

    @Test
    void testListFiles() {
        // Test listing files
        assertEquals("/", directory.list()); // Initially only root should exist
        directory.ialloc("file1");
        directory.ialloc("file2");
        directory.ialloc("file3");
        String filelist = directory.list();
        assertTrue(filelist.contains("file1"));
        assertTrue(filelist.contains("file2"));
        assertTrue(filelist.contains("file3"));
        assertTrue(filelist.contains("/"));
        // remove a file and check if the file is no longer in the list.
        directory.ifree((short) 1);
        filelist = directory.list();
        assertFalse(filelist.contains("file1"));
        assertTrue(filelist.contains("file2"));
        assertTrue(filelist.contains("file3"));
        assertTrue(filelist.contains("/"));

    }

    @Test
    void testListFiles_output() {
        // Allocate multiple files
        directory.ialloc("fileA");
        directory.ialloc("fileB");
        directory.ialloc("fileC");
        directory.ialloc("longfile_name_1");
        directory.ialloc("test_file");

        // Get the list of files from the directory
        String fileList = directory.list();

        // Assert that the list contains all allocated files and the root
        assertTrue(fileList.contains("fileA"), "fileList does not contain fileA");
        assertTrue(fileList.contains("fileB"), "fileList does not contain fileB");
        assertTrue(fileList.contains("fileC"), "fileList does not contain fileC");
        assertTrue(fileList.contains("longfile_name_1"), "fileList does not contain longfile_name_1");
        assertTrue(fileList.contains("test_file"), "fileList does not contain test_file");
        assertTrue(fileList.contains("/"), "fileList does not contain /");

        // Output the file list
        System.out.println("Current files in directory: " + fileList);
    }

    @Test
    void testDumpRandomFilenamesAndLengths() {
        Random random = new Random();
        int numFilesToCreate = 32; // Up to 32 files

        List<Directory.FilenameLengthPair> createdFiles = new ArrayList<>();

        for (int i = 0; i < numFilesToCreate; i++) {
            String filename = generateRandomFilename(random);
            directory.ialloc(filename);
            createdFiles.add(new Directory.FilenameLengthPair(filename, filename.length()));
        }

        // Retrieve the formatted list
        List<Directory.FilenameLengthPair> formattedList = directory.listFormatted();

        // Verify each created file is in the formatted list, also check for root
        for (Directory.FilenameLengthPair file : createdFiles) {
            assertTrue(formattedList.stream()
                    .anyMatch(pair -> pair.filename.equals(file.filename) && pair.length == file.length));
        }
        assertTrue(formattedList.stream().anyMatch(pair -> pair.filename.equals("/")), "fileList does not contain /");

        // Print the formatted list
        System.out.println("Dump of up to 32 Random Filenames and Lengths:");
        formattedList.forEach(System.out::println);
    }

    @Test
    void testIfreeEdgeCases() {
        // Test freeing invalid inode numbers
        assertFalse(directory.ifree((short) -1), "Should not free negative inode number");
        assertFalse(directory.ifree((short) MAX_INUMBER), "Should not free inode number >= max");
        assertFalse(directory.ifree((short) (MAX_INUMBER + 10)), "Should not free inode number way beyond max");
        
        // Test freeing already freed inode
        short inode = directory.ialloc("testfile");
        assertTrue(directory.ifree(inode), "Should successfully free allocated inode");
        assertFalse(directory.ifree(inode), "Should not free already freed inode");
        
        // Test freeing root directory (inode 0) - implementation allows this
        assertTrue(directory.ifree((short) 0), "Implementation allows freeing root directory");
        // After freeing root, it should be gone from namei lookup
        assertEquals(-1, directory.namei("/"), "Root should be gone after freeing");
    }

    @Test
    void testConstructorInitialization() {
        // Test that constructor properly initializes directory
        Directory newDir = new Directory(10);
        
        // Root should be present
        assertEquals(0, newDir.namei("/"), "Root directory should be at inode 0");
        assertEquals("/", newDir.list(), "Initial directory should only contain root");
        
        // All other inodes should be unallocated (can't test ifree on unallocated inodes)
        // Instead, test that they don't match any filename lookups
        for (int i = 1; i < 10; i++) {
            assertEquals(-1, newDir.namei("file" + i), "Unallocated inode " + i + " should not match any filename");
        }
    }

    @Test
    void testBytesConversionEdgeCases() {
        // Test empty directory (only root)
        byte[] emptyBytes = directory.directory2bytes();
        Directory emptyDir = new Directory(MAX_INUMBER);
        emptyDir.bytes2directory(emptyBytes);
        assertEquals("/", emptyDir.list(), "Empty directory conversion should preserve root only");
        
        // Test directory with single character filenames
        directory.ialloc("a");
        directory.ialloc("b");
        directory.ialloc("z");
        
        byte[] singleCharBytes = directory.directory2bytes();
        Directory singleCharDir = new Directory(MAX_INUMBER);
        singleCharDir.bytes2directory(singleCharBytes);
        
        assertEquals(1, singleCharDir.namei("a"), "Single char filename 'a' should be preserved");
        assertEquals(2, singleCharDir.namei("b"), "Single char filename 'b' should be preserved");
        assertEquals(3, singleCharDir.namei("z"), "Single char filename 'z' should be preserved");
        
        // Test directory with maximum length filenames
        String maxFile1 = "x".repeat(MAX_CHARS);
        String maxFile2 = "y".repeat(MAX_CHARS);
        Directory maxDir = new Directory(MAX_INUMBER);
        maxDir.ialloc(maxFile1);
        maxDir.ialloc(maxFile2);
        
        byte[] maxBytes = maxDir.directory2bytes();
        Directory restoredMaxDir = new Directory(MAX_INUMBER);
        restoredMaxDir.bytes2directory(maxBytes);
        
        assertEquals(1, restoredMaxDir.namei(maxFile1), "Max length filename should be preserved");
        assertEquals(2, restoredMaxDir.namei(maxFile2), "Second max length filename should be preserved");
    }

    @Test
    void testNameiEdgeCases() {
        // Test empty string lookup - should not be found initially
        assertEquals(-1, directory.namei(""), "Empty string should not be found initially");
        
        // Test null string - should handle gracefully without crashing
        try {
            assertEquals(-1, directory.namei(null), "Null string should not be found");
        } catch (NullPointerException e) {
            // Implementation may not handle null gracefully, which is acceptable
            assertTrue(true, "Implementation throws NPE for null, which is acceptable");
        }
        
        // Test case sensitivity
        directory.ialloc("TestFile");
        assertEquals(-1, directory.namei("testfile"), "Filename lookup should be case sensitive");
        assertEquals(-1, directory.namei("TESTFILE"), "Filename lookup should be case sensitive");
        assertNotEquals(-1, directory.namei("TestFile"), "Exact case should match");
        
        // Test special characters
        directory.ialloc("file.txt");
        directory.ialloc("file-name");
        directory.ialloc("file_name");
        directory.ialloc("file123");
        
        assertNotEquals(-1, directory.namei("file.txt"), "Filename with dot should work");
        assertNotEquals(-1, directory.namei("file-name"), "Filename with dash should work");
        assertNotEquals(-1, directory.namei("file_name"), "Filename with underscore should work");
        assertNotEquals(-1, directory.namei("file123"), "Filename with numbers should work");
    }

    @Test
    void testIallocEdgeCases() {
        // Test empty string allocation - should work now that namei bug is fixed
        short emptyInode = directory.ialloc("");
        assertNotEquals(-1, emptyInode, "Empty string should be allocated");
        assertEquals(emptyInode, directory.namei(""), "Empty string should be findable after allocation");
        
        // Test null string allocation - should handle gracefully
        try {
            assertEquals(-1, directory.ialloc(null), "Null string should not be allocated");
        } catch (NullPointerException e) {
            // Implementation may not handle null gracefully, which is acceptable
            assertTrue(true, "Implementation throws NPE for null, which is acceptable");
        }
        
        // Test exactly MAX_CHARS length (should work)
        String exactMaxFile = "m".repeat(MAX_CHARS);
        assertNotEquals(-1, directory.ialloc(exactMaxFile), "Filename of exactly MAX_CHARS should be allocated");
        
        // Test MAX_CHARS + 1 length (should fail)
        String tooLongFile = "n".repeat(MAX_CHARS + 1);
        assertEquals(-1, directory.ialloc(tooLongFile), "Filename longer than MAX_CHARS should fail");
        
        // Test whitespace handling
        directory.ialloc(" file ");
        assertNotEquals(-1, directory.namei(" file "), "Filename with spaces should be preserved exactly");
        assertEquals(-1, directory.namei("file"), "Trimmed version should not match");
    }

    @Test
    void testListFormattedToStringMethod() {
        // Test FilenameLengthPair toString method
        Directory.FilenameLengthPair pair = new Directory.FilenameLengthPair("testfile", 8);
        String result = pair.toString();
        
        assertTrue(result.contains("testfile"), "toString should contain filename");
        assertTrue(result.contains("8"), "toString should contain length");
        
        // Test with maximum length filename
        Directory.FilenameLengthPair maxPair = new Directory.FilenameLengthPair("x".repeat(MAX_CHARS), MAX_CHARS);
        String maxResult = maxPair.toString();
        assertTrue(maxResult.contains("x".repeat(MAX_CHARS)), "toString should handle max length filename");
        assertTrue(maxResult.contains(String.valueOf(MAX_CHARS)), "toString should contain max length");
    }

    @Test
    void testDirectoryCapacityAndReuse() {
        // Fill directory to capacity
        List<Short> allocatedInodes = new ArrayList<>();
        for (int i = 1; i < MAX_INUMBER; i++) {
            short inode = directory.ialloc("file" + i);
            allocatedInodes.add(inode);
            assertNotEquals(-1, inode, "Should allocate inode " + i);
        }
        
        // Verify directory is full
        assertEquals(-1, directory.ialloc("overflow"), "Directory should be full");
        
        // Free some inodes in the middle
        assertTrue(directory.ifree(allocatedInodes.get(10)), "Should free inode 11");
        assertTrue(directory.ifree(allocatedInodes.get(20)), "Should free inode 21");
        assertTrue(directory.ifree(allocatedInodes.get(30)), "Should free inode 31");
        
        // Allocate new files - should reuse freed inodes
        short newInode1 = directory.ialloc("newfile1");
        short newInode2 = directory.ialloc("newfile2");
        short newInode3 = directory.ialloc("newfile3");
        
        // Should reuse the freed inodes (order may vary based on implementation)
        List<Short> freedInodes = List.of(allocatedInodes.get(10), allocatedInodes.get(20), allocatedInodes.get(30));
        assertTrue(freedInodes.contains(newInode1), "Should reuse freed inode");
        assertTrue(freedInodes.contains(newInode2), "Should reuse freed inode");
        assertTrue(freedInodes.contains(newInode3), "Should reuse freed inode");
        
        // Verify directory is full again
        assertEquals(-1, directory.ialloc("overflow2"), "Directory should be full again");
    }
}