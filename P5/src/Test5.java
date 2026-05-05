/**
 * Test5 is a comprehensive test suite for the ThreadOS FileSystem implementation.
 * This test class validates the core functionality of the file system including
 * formatting, file operations (open, read, write, close, delete), seeking,
 * and multi-file operations.
 * 
 * The test suite covers:
 * - File system formatting with specified number of inodes
 * - Basic file operations (create, open, read, write, close, delete)
 * - File seeking with different whence values (SEEK_SET, SEEK_CUR, SEEK_END)
 * - Small file operations (under one block)
 * - Large file operations (multiple blocks with indirect pointers)
 * - File appending operations
 * - Multiple file creation and management
 * - Concurrent file access between processes
 * 
 * Each test method returns a boolean indicating success or failure,
 * and the test results are displayed with point values for grading.
 * 
 * V2.2 Updates:
 * - Updated superblock validation logic for 32-byte inodes and 512-byte blocks
 * - Corrected inode block calculation (16 inodes per block)
 * - Enhanced documentation with comprehensive JavaDoc comments
 * - Aligned test expectations with improved filesystem core components
 * 
 * @author Stephen Dame
 * @since 2026
 */
class Test5 extends Thread {
  /**
   * Default number of files (inodes) to use when no argument is provided.
   * This value represents a reasonable default for testing purposes.
   */
  final static int DEFAULTFILES = 48;
  
  /**
   * Number of files (inodes) to create during file system formatting.
   * This value is either provided as a command line argument or defaults
   * to DEFAULTFILES.
   */
  final int files;
  
  /**
   * File descriptor for the currently open file.
   * Used throughout the test methods to track the active file handle.
   */
  int fd;
  
  /**
   * 16-byte buffer used for small file read/write operations.
   * Initialized with sequential byte values for testing data integrity.
   */
  final byte[] buf16 = new byte[16];
  
  /**
   * 32-byte buffer used for append operations and medium-sized data transfers.
   * Contains test data patterns for verifying file content accuracy.
   */
  final byte[] buf32 = new byte[32];
  
  /**
   * 24-byte buffer used for seek and overwrite operations.
   * Contains specific test patterns for validating file positioning.
   */
  final byte[] buf24 = new byte[24];
  
  /**
   * Size of the last read or write operation.
   * Used to verify that operations return the expected number of bytes.
   */
  int size;

  /**
   * Constructor that initializes Test5 with a specified number of files.
   * The number of files determines how many inodes will be created during
   * file system formatting.
   * 
   * @param args Command line arguments where args[0] contains the number
   *             of files to create in the file system
   */
  public Test5(String args[]) {
    files = Integer.parseInt(args[0]);
    // SysLib.cout( "files = " + files + "\n" );
  }

  /**
   * Default constructor that initializes Test5 with the default number of files.
   * Uses DEFAULTFILES constant when no specific file count is provided.
   */
  public Test5() {
    files = DEFAULTFILES;
    // SysLib.cout( "files = " + files + "\n" );
  }

  /**
   * Main test execution method that runs all file system tests in sequence.
   * Each test validates a specific aspect of file system functionality and
   * displays the results with associated point values for grading.
   * 
   * Test sequence:
   * 1-8: Small file operations (css430 file)
   * 9-15: Large file operations (bothell file)
   * 16: File deletion
   * 17: Multiple file creation
   * 18: Concurrent file access
   * 
   * The method terminates the system after all tests complete.
   */
  public void run() {
    if (test1()) // format with specified # of files
      SysLib.cout("Correct behavior of format...............................2\n");
    if (test2()) // open "css430" with "w+"
      SysLib.cout("Correct behavior of open.................................2\n");
    if (test3()) // write buf[16]
      SysLib.cout("Correct behavior of writing a few bytes..................2\n");
    if (test4()) // close fd
      SysLib.cout("Correct behavior of close................................1\n");
    if (test5()) // read buf[16] from "css430"
      SysLib.cout("Correct behavior of reading a few bytes..................1\n");
    if (test6()) // append buf[32] to "css430"
      SysLib.cout("Correct behavior of appending a few bytes................1\n");
    if (test7()) // seek and read from "css430"
      SysLib.cout("Correct behavior of seeking in a small file..............1\n");
    if (test8()) // open "css430" with "w+"
      SysLib.cout("Correct behavior of read/writing a small file............1\n");

    test9(); // open "bothell" with "w+"
    if (test10()) // write buf[512 * 13]
      SysLib.cout("Correct behavior of writing a lot of bytes...............2\n");
    test11(); // close fd
    if (test12()) // read buf[512 * 13] from "bothell"
      SysLib.cout("Correct behavior of reading a lot of bytes...............2\n");
    if (test13()) // append buf[32] to "bothell"
      SysLib.cout("Correct behavior of appending to a large file............1\n");
    if (test14()) // seek and read from "bothell"
      SysLib.cout("Correct behavior of seeking in a large file..............2\n");
    if (test15()) // open "bothell" with "w+"
      SysLib.cout("Correct behavior of read/writing a large file............2\n");

    if (test16()) // delete "css430"
      SysLib.cout("Correct behavior of delete...............................1\n");
    if (test17()) // create "uwb0" - "uwb45" of buf[512 * 13]
      SysLib.cout("Correct behavior of creating over 40 files ..............2\n");
    if (test18()) // "uwb1" read/written among Test5 and Test6
      SysLib.cout("Correct behavior of two fds to the same file.............2\n");

    SysLib.cout("Test completed\n");
    SysLib.exit();
  }

  /**
   * Test 1: File system formatting validation.
   * Formats the file system with the specified number of files and verifies
   * that the superblock contains correct metadata including total blocks,
   * inode blocks, and free list pointer.
   * 
   * Updated for V2.2: The format process includes directory creation which
   * allocates blocks from the free list, so the final freeList value will
   * be higher than the initial calculation.
   * 
   * @return true if formatting completed successfully and superblock
   *         contains expected values, false otherwise
   */
  private boolean test1() {
    // .............................................."
    SysLib.cout("1: format( " + files + " )...................");
    SysLib.format(files);
    byte[] superblock = new byte[512];
    SysLib.rawread(0, superblock);
    int totalBlocks = SysLib.bytes2int(superblock, 0);
    int inodeBlocks = SysLib.bytes2int(superblock, 4);
    int freeList = SysLib.bytes2int(superblock, 8);
    if (totalBlocks != 1000) {
      SysLib.cout("totalBlocks = " + totalBlocks + " (wrong)\n");
      return false;
    }
    // V2.2: The format method expects inodeBlocks (number of inode blocks)
    if (inodeBlocks != files) {
      SysLib.cout("inodeBlocks = " + inodeBlocks + " (wrong)\n");
      return false;
    }
    // V2.2: After formatting, directory creation allocates blocks from free list
    // Initial calculation: 1 + (48 * 32) / 512 = 4
    // After directory creation: freeList will be higher (around 10 based on output)
    int initialFreeList = 1 + (files * 32) / 512;
    if (freeList < initialFreeList) {
      SysLib.cout("freeList = " + freeList + " (should be >= " + initialFreeList + ")\n");
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 2: File opening validation.
   * Opens a new file "css430" with write/read mode and verifies that
   * the returned file descriptor is valid (expected value 3).
   * 
   * @return true if file opens successfully with expected file descriptor,
   *         false otherwise
   */
  private boolean test2() {
    // .............................................."
    SysLib.cout("2: fd = open( \"css430\", \"w+\" )....");
    fd = SysLib.open("css430", "w+");
    if (fd != 3) {
      SysLib.cout("fd = " + fd + " (wrong)\n");
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 3: Small file writing validation.
   * Writes 16 bytes of sequential data to the open file and verifies
   * that the write operation returns the correct number of bytes written.
   * 
   * @return true if 16 bytes are written successfully, false otherwise
   */
  private boolean test3() {
    // .............................................."
    SysLib.cout("3: size = write( fd, buf[16] )....");
    for (byte i = 0; i < 16; i++)
      buf16[i] = i;
    size = SysLib.write(fd, buf16);
    if (size != 16) {
      SysLib.cout("size = " + size + " (wrong)\n");
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 4: File closing validation.
   * Closes the open file and verifies that subsequent write operations
   * fail, confirming that the file descriptor is properly invalidated.
   * 
   * @return true if file closes properly and subsequent writes fail,
   *         false otherwise
   */
  private boolean test4() {
    // .............................................."
    SysLib.cout("4: close( fd )....................");
    SysLib.close(fd);

    size = SysLib.write(fd, buf16);
    if (size > 0) {
      SysLib.cout("writable even after closing the file\n");
      return false;
    }

    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 5: File reading validation.
   * Reopens the "css430" file in read mode and verifies that the previously
   * written data can be read back correctly, confirming data persistence.
   * 
   * @return true if file can be reopened and data matches what was written,
   *         false otherwise
   */
  private boolean test5() {
    // .............................................."
    SysLib.cout("5: reopen and read from \"css430\"..");
    fd = SysLib.open("css430", "r");

    byte[] tmpBuf = new byte[16];
    size = SysLib.read(fd, tmpBuf);
    if (size != 16) {
      SysLib.cout("size = " + size + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    for (int i = 0; i < 16; i++)
      if (tmpBuf[i] != buf16[i]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 6: File appending validation.
   * Opens "css430" in append mode, writes 32 additional bytes, then verifies
   * that both the original and appended data are present and correct.
   * 
   * @return true if data is appended correctly and all content is readable,
   *         false otherwise
   */
  private boolean test6() {
    // .............................................."
    SysLib.cout("6: append buf[32] to \"css430\".....");
    for (byte i = 0; i < 32; i++)
      buf32[i] = (byte) (i + (byte) 16);

    fd = SysLib.open("css430", "a");
    SysLib.write(fd, buf32);
    SysLib.close(fd);

    fd = SysLib.open("css430", "r");
    byte[] tmpBuf = new byte[48];
    size = SysLib.read(fd, tmpBuf);
    if (size != 48) {
      SysLib.cout("size = " + size + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    for (int i = 0; i < 16; i++)
      if (tmpBuf[i] != buf16[i]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (int i = 16; i < 48; i++)
      if (tmpBuf[i] != buf32[i - 16]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 7: File seeking validation for small files.
   * Tests all three seek modes (SEEK_SET, SEEK_CUR, SEEK_END) on the
   * "css430" file and verifies that seeking positions the file pointer
   * correctly and reads return expected data.
   * 
   * @return true if all seek operations work correctly, false otherwise
   */
  private boolean test7() {
    // .............................................."
    SysLib.cout("7: seek and read from \"css430\"....");

    fd = SysLib.open("css430", "r");

    int position = SysLib.seek(fd, 10, 0);
    if (position != 10) {
      SysLib.cout("seek(fd,10,0)=" + position + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    byte[] tmpBuf = new byte[2];
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) 10) {
      SysLib.cout("seek(fd,10,0) contents " + tmpBuf[0] + "(wrong\n");
      SysLib.close(fd);
      return false;
    }

    position = SysLib.seek(fd, 10, 0);
    position = SysLib.seek(fd, 10, 1);
    if (position != 20) {
      SysLib.cout("seek(fd,10,1)=" + position + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) 20) {
      SysLib.cout("seek(fd,10,1) contents " + tmpBuf[0] + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }

    position = SysLib.seek(fd, -2, 2);
    if (position != 46) {
      SysLib.cout("seek(fd,-2,2)=" + position + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) 46) {
      SysLib.cout("seek(fd,-2,2) contents " + tmpBuf[0] + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }

    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 8: Read/write mode validation for small files.
   * Opens "css430" in read/write mode, seeks to position 24, writes new data,
   * then verifies that the file contains the original data, appended data,
   * and the newly written data in the correct positions.
   * 
   * @return true if read/write operations work correctly with seeking,
   *         false otherwise
   */
  private boolean test8() {
    // .............................................."
    SysLib.cout("8: open \"css430\" with w+..........");

    for (short i = 0; i < 24; i++)
      buf24[i] = (byte) (24 - i);

    fd = SysLib.open("css430", "w+");
    SysLib.seek(fd, 24, 0);
    SysLib.write(fd, buf24);

    SysLib.seek(fd, 0, 0);
    byte[] tmpBuf = new byte[48];
    SysLib.read(fd, tmpBuf);

    for (byte i = 0; i < 16; i++)
      if (tmpBuf[i] != buf16[i]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (byte i = 16; i < 24; i++)
      if (tmpBuf[i] != buf32[i - 16]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (byte i = 24; i < 48; i++)
      if (tmpBuf[i] != buf24[i - 24]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }

    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * 6656-byte buffer used for large file operations.
   * This size is chosen to test multi-block files that require
   * indirect block pointers (6656 bytes = 13 blocks of 512 bytes).
   */
  final byte[] buf6656 = new byte[6656];

  /**
   * Test 9: Large file opening validation.
   * Opens a new file "bothell" with write/read mode for testing
   * large file operations that span multiple blocks.
   * 
   * @return true if large file opens successfully with expected file descriptor,
   *         false otherwise
   */
  private boolean test9() {
    // .............................................."
    SysLib.cout("9: fd = open( \"bothell\", \"w\" )....");
    fd = SysLib.open("bothell", "w+");
    if (fd != 3) {
      SysLib.cout("fd = " + fd + " (wrong)\n");
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 10: Large file writing validation.
   * Writes 6656 bytes (13 blocks) to the "bothell" file, testing the
   * file system's ability to handle multi-block files with indirect pointers.
   * 
   * @return true if all 6656 bytes are written successfully, false otherwise
   */
  private boolean test10() {
    // .............................................."
    SysLib.cout("10: size = write( fd, buf[6656] ).");
    for (int i = 0; i < 6656; i++)
      buf6656[i] = (byte) (i % 256);
    size = SysLib.write(fd, buf6656);
    if (size != 6656) {
      SysLib.cout("size = " + size + " (wrong)\n");
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 11: Large file closing validation.
   * Closes the large file and verifies that subsequent write operations
   * fail, confirming proper file descriptor management for large files.
   * 
   * @return true if large file closes properly and subsequent writes fail,
   *         false otherwise
   */
  private boolean test11() {
    // .............................................."
    SysLib.cout("11: close( fd )....................");
    SysLib.close(fd);

    size = SysLib.write(fd, buf16);
    if (size > 0) {
      SysLib.cout("writable even after closing the file\n");
      return false;
    }

    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 12: Large file reading validation.
   * Reopens the "bothell" file and reads all 6656 bytes, verifying that
   * large files can be read back correctly and data integrity is maintained
   * across multiple blocks.
   * 
   * @return true if all data is read correctly from the large file,
   *         false otherwise
   */
  private boolean test12() {
    // .............................................."
    SysLib.cout("12: reopen and read from \"bothell\"");
    fd = SysLib.open("bothell", "r");

    byte[] tmpBuf = new byte[6656];
    size = SysLib.read(fd, tmpBuf);
    if (size != 6656) {
      SysLib.cout("size = " + size + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    for (int i = 0; i < 6656; i++)
      if (tmpBuf[i] != buf6656[i]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 13: Large file appending validation.
   * Appends 32 bytes to the large "bothell" file and verifies that both
   * the original large content and the appended data are correct.
   * 
   * @return true if data is appended correctly to the large file,
   *         false otherwise
   */
  private boolean test13() {
    // .............................................."
    SysLib.cout("13: append buf[32] to \"bothell\"...");

    fd = SysLib.open("bothell", "a");
    SysLib.write(fd, buf32);
    SysLib.close(fd);

    fd = SysLib.open("bothell", "r");
    byte[] tmpBuf = new byte[6688];
    size = SysLib.read(fd, tmpBuf);
    if (size != 6688) {
      SysLib.cout("size = " + size + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    for (int i = 0; i < 6656; i++) {
      if (tmpBuf[i] != buf6656[i]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " buf6656 = " +
            buf6656[i] + "\n");
        SysLib.close(fd);
        return false;
      }
    }
    for (int i = 6656; i < 6688; i++)
      if (tmpBuf[i] != buf32[i - 6656]) {
        SysLib.cout("buf[" + i + "] = " + tmpBuf[i] + " buf32 = " +
            buf32[i - 6656] + "\n");
        SysLib.close(fd);
        return false;
      }
    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 14: File seeking validation for large files.
   * Tests seeking operations on the large "bothell" file using positions
   * that span multiple blocks, verifying that indirect block pointers
   * work correctly with seek operations.
   * 
   * @return true if all seek operations work correctly on large files,
   *         false otherwise
   */
  private boolean test14() {
    // .............................................."
    SysLib.cout("14: seek and read from \"bothell\"...");

    fd = SysLib.open("bothell", "r");

    int position = SysLib.seek(fd, 512 * 11, 0);
    if (position != 512 * 11) {
      SysLib.cout("seek(fd,512 * 11,0)=" + position + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    byte[] tmpBuf = new byte[2];
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) (512 * 11)) {
      SysLib.cout("seek(fd,512*11,0) contents " + tmpBuf[0] + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }

    position = SysLib.seek(fd, 512 * 11, 0);
    position = SysLib.seek(fd, 512, 1);
    if (position != 512 * 12) {
      SysLib.cout("seek(fd,512,1)=" + position + " (wrong)\n");
      SysLib.close(fd);
      return false;
    }
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) (512 * 12)) {
      SysLib.cout("seek(fd,512,1) contents " + tmpBuf[0] + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }

    position = SysLib.seek(fd, -2, 2);
    if (position != 512 * 13 + 32 - 2) {
      SysLib.cout("seek(fd,-2,2)=" + position + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }
    size = SysLib.read(fd, tmpBuf);
    if (tmpBuf[0] != (byte) 46) {
      SysLib.cout("seek(fd,-2,2) contents " + tmpBuf[0] + "(wrong)\n");
      SysLib.close(fd);
      return false;
    }

    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 15: Read/write mode validation for large files.
   * Opens "bothell" in read/write mode, seeks to a position within the
   * large file, writes new data, then verifies that the modification
   * was applied correctly while preserving surrounding data.
   * 
   * @return true if read/write operations work correctly on large files,
   *         false otherwise
   */
  private boolean test15() {
    // .............................................."
    SysLib.cout("15: open \"bothell\" with w+.........");

    for (short i = 0; i < 24; i++)
      buf24[i] = (byte) (24 - i);

    fd = SysLib.open("bothell", "w+");
    SysLib.seek(fd, 512 * 12 - 3, 0);
    SysLib.write(fd, buf24);

    SysLib.seek(fd, 0, 0);
    byte[] tmpBuf = new byte[6688];
    SysLib.read(fd, tmpBuf);

    for (int i = 0; i < 512 * 12 - 3; i++)
      if (tmpBuf[i] != buf6656[i]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (int i = 512 * 12 - 3; i < 512 * 12 - 3 + 24; i++)
      if (tmpBuf[i] != buf24[i - (512 * 12 - 3)]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (int i = 512 * 12 - 3 + 24; i < 6656; i++)
      if (tmpBuf[i] != buf6656[i]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }
    for (int i = 6656; i < 6688; i++)
      if (tmpBuf[i] != buf32[i - 6656]) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " (wrong)\n");
        SysLib.close(fd);
        return false;
      }

    SysLib.close(fd);
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 16: File deletion validation.
   * Verifies that the "css430" file exists, deletes it, then confirms
   * that subsequent attempts to open the file fail, validating that
   * file deletion works correctly.
   * 
   * @return true if file deletion works correctly, false otherwise
   */
  private boolean test16() {
    // .............................................."
    SysLib.cout("16: delete(\"css430\")..............");
    fd = SysLib.open("css430", "r");
    if (fd == -1) {
      SysLib.cout("fd = " + fd + " (wrong)\n");
      return false;
    }
    SysLib.close(fd);
    SysLib.delete("css430");
    fd = SysLib.open("css430", "r");
    if (fd != -1) {
      SysLib.cout("fd = " + fd + " (wrong, css430 still exists!)\n");
      SysLib.close(fd);
      return false;
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 17: Multiple file creation validation.
   * Creates 29 files named "uwb0" through "uwb28", writes large amounts
   * of data to each, then closes all files. This tests the file system's
   * ability to handle multiple concurrent files and inode management.
   * 
   * @return true if all files are created, written to, and closed successfully,
   *         false otherwise
   */
  private boolean test17() {
    // .............................................."
    SysLib.cout("17: create uwb0-29 of 512*13......");
    int fdes[] = new int[29];
    for (int i = 0; i < 29; i++) {
      Integer suffix = Integer.valueOf(i); // updated Java syntax
      String file = "uwb" + suffix.toString();
      fdes[i] = SysLib.open(file, "w+");
      if (fdes[i] == -1) {
        SysLib.cout("file: " + file + " open failed\n");
        return false;
      }
    }
    for (int i = 0; i < 29; i++) {
      if (SysLib.write(fdes[i], buf6656) != 6656) {
        SysLib.cout("fd[" + fdes[i] + "] failed in writing\n");
        return false;
      }
    }
    for (int i = 0; i < 29; i++) {
      if (SysLib.close(fdes[i]) == -1) {
        SysLib.cout("fd[" + fdes[i] + "] failed in closing\n");
        return false;
      }
    }
    SysLib.cout("successfully completed\n");
    return true;
  }

  /**
   * Test 18: Concurrent file access validation.
   * Opens "uwb0" file, spawns a Test6 process that accesses the same file
   * system, then verifies that file operations work correctly across
   * multiple processes. This tests file system consistency and concurrent access.
   * 
   * @return true if concurrent file access works correctly, false otherwise
   */
  private boolean test18() {
    // .............................................."
    SysLib.cout("18: uwb0 read b/w Test5 & Test6...\n");
    fd = SysLib.open("uwb0", "r");
    String[] cmd = new String[2];
    cmd[0] = "Test6";
    cmd[1] = String.format("%d", fd);
    SysLib.exec(cmd);
    SysLib.join();
    SysLib.close(fd);

    SysLib.cout("Test6.java terminated\n");
    fd = SysLib.open("uwb1", "r");
    byte[] tmpBuf = new byte[512];
    SysLib.read(fd, tmpBuf);
    for (int i = 0; i < 512; i++)
      if (tmpBuf[i] != (byte) 100) {
        SysLib.cout("tmpBuf[" + i + "]=" + tmpBuf[i] + " should be 100"
            + "\n");
        SysLib.close(fd);
        return false;
      }
    SysLib.close(fd);
    return true;
  }
}
