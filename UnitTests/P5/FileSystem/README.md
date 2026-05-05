# FileSystem Unit Test Assignment

## Overview

This assignment requires you to implement a complete file system that provides UNIX-like file operations. The FileSystem class serves as the main interface for all file system operations and coordinates between the SuperBlock, Directory, FileTable, and Inode components.

## Learning Objectives

By completing this assignment, you will:
- Understand how file systems manage files, directories, and storage blocks
- Learn about file operations (open, close, read, write, seek, delete)
- Implement proper synchronization and thread safety
- Handle different file access modes (read, write, append)
- Manage file system metadata and consistency

## Assignment Structure

```
FileSystem/
├── src/
│   ├── main/java/
│   │   └── FileSystem.java          # Your implementation goes here
│   └── test/java/
│       └── FileSystemTest.java      # Comprehensive test suite (15 tests)
├── lib/
│   └── mock.jar                     # Mock implementations of dependencies
├── pom.xml                          # Maven build configuration
└── README.md                        # This file
```

## Dependencies (Provided in mock.jar)

The following classes are provided as mocks and simulate the behavior of the actual file system components:

- **SuperBlock**: Manages free blocks and file system metadata
- **Directory**: Maps filenames to inode numbers
- **FileTable**: Tracks open files and manages access modes
- **FileTableEntry**: Represents an entry in the file table
- **Inode**: Stores file metadata and block pointers
- **Disk**: Simulates disk storage operations
- **SysLib**: Provides system-level utilities
- **Constants**: Defines system constants
- **LoggingSetup**: Provides logging utilities

## Implementation Requirements

### Core Methods to Implement

1. **Constructor: `FileSystem(int diskBlocks)`**
   - Initialize SuperBlock, Directory, and FileTable
   - Set up the basic file system structure

2. **File Operations**
   - `open(String filename, String mode)`: Open files in read ("r"), write ("w"), or append ("a") mode
   - `close(FileTableEntry ftEnt)`: Close files and manage reference counting
   - `read(FileTableEntry ftEnt, byte[] buffer)`: Read data from files
   - `write(FileTableEntry ftEnt, byte[] buffer)`: Write data to files
   - `delete(String filename)`: Delete files from the file system

3. **File System Operations**
   - `format(int files)`: Format the file system with specified number of inodes
   - `sync()`: Synchronize file system data to disk
   - `fsize(FileTableEntry ftEnt)`: Get file size
   - `seek(FileTableEntry ftEnt, int offset, int whence)`: Reposition file pointer

4. **Helper Methods**
   - `deallocAllBlocks(FileTableEntry ftEnt)`: Free all blocks associated with a file

### Key Implementation Details

#### File Access Modes
- **Read ("r")**: File must exist, read-only access
- **Write ("w")**: Creates new file or truncates existing, write access
- **Append ("a")**: Creates new file or appends to existing, write access at end

#### Synchronization Requirements
- Use proper synchronization on inode objects
- Handle concurrent access to shared resources
- Prevent deadlocks and race conditions

#### Block Management
- Allocate blocks from SuperBlock when needed
- Free blocks when files are deleted or truncated
- Handle both direct and indirect block addressing

#### Error Handling
- Return null for invalid operations
- Handle non-existent files appropriately
- Validate parameters and modes

## Test Suite

The provided test suite (`FileSystemTest.java`) includes 15 comprehensive tests:

1. **testCreateFileSystem**: Basic object creation
2. **testFormat**: File system formatting
3. **testOpenNewFile**: Opening new files for writing
4. **testOpenReadFail**: Handling non-existent files in read mode
5. **testOpenAppendClose**: Append mode functionality
6. **testSync**: File system synchronization
7. **testOpenWriteAgainClose**: Reopening files
8. **testBasicFileOperations**: Basic file creation and properties
9. **testRead**: Reading data from files
10. **testFsize**: File size reporting
11. **testDelete**: File deletion
12. **testSeek**: File pointer positioning
13. **testMultipleFiles**: Handling multiple simultaneous files
14. **testAppendMode**: Comprehensive append mode testing
15. **testErrorConditions**: Error handling and edge cases

## Running the Tests

```bash
# Compile and run all tests
mvn clean test

# Run a specific test
mvn test -Dtest=FileSystemTest#testOpenNewFile

# Compile only (to check for syntax errors)
mvn compile
```

## Implementation Strategy

### Phase 1: Basic Structure
1. Implement the constructor to initialize components
2. Implement `fsize()` as it's the simplest method
3. Implement `format()` to set up the file system

### Phase 2: File Operations
1. Implement `open()` for basic file creation
2. Implement `close()` with proper reference counting
3. Add `sync()` for data persistence

### Phase 3: Data Operations
1. Implement `write()` for basic data writing
2. Implement `read()` for data retrieval
3. Add `seek()` for file positioning

### Phase 4: Advanced Features
1. Implement `delete()` for file removal
2. Add `deallocAllBlocks()` for proper cleanup
3. Handle all file modes (read, write, append)

### Phase 5: Error Handling and Edge Cases
1. Add proper error checking and validation
2. Handle concurrent access scenarios
3. Test with multiple files and complex operations

## Common Pitfalls to Avoid

1. **Double Reference Counting**: Don't increment inode.count in both `open()` and `FileTable.falloc()`
2. **Block Deallocation on Close**: Only deallocate blocks on delete or write mode open, not on close
3. **Root Directory Handling**: Handle the root directory (inode 0) specially
4. **Synchronization**: Always synchronize on inode objects for thread safety
5. **Mode Validation**: Check file modes before performing operations

## Debugging Tips

1. **Use Logging**: The LoggingSetup class provides debugging capabilities
2. **Test Incrementally**: Implement and test one method at a time
3. **Check Reference Counts**: Monitor inode.count values during operations
4. **Verify Block Allocation**: Ensure blocks are properly allocated and freed
5. **Test Edge Cases**: Try empty files, large files, and concurrent operations

## Success Criteria

Your implementation is successful when:
- All 15 tests pass consistently
- No deadlocks or infinite loops occur
- Proper resource management (no memory/block leaks)
- Thread-safe operation under concurrent access
- Correct handling of all file modes and operations

## Submission

Submit your completed `FileSystem.java` file. The file should:
- Contain complete implementations of all required methods
- Include proper error handling and validation
- Pass all provided unit tests
- Follow good coding practices and documentation standards

Good luck with your implementation!