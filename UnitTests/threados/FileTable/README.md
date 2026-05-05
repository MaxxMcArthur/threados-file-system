# FileTable Student Assignment

## Overview
Your task is to implement the `FileTable` class that manages file table entries in a filesystem. The file table tracks all open files and their access modes.

## Project Structure
```
.
├── lib/
│   └── mock.jar              # Mock dependencies (Directory, Inode, FileTableEntry)
├── src/
│   ├── main/java/
│   │   └── FileTable.java    # YOUR IMPLEMENTATION GOES HERE
│   └── test/java/
│       └── FileTableUnitTest.java # Complete test suite
├── pom.xml                   # Maven configuration
└── README.md                 # This file
```

## Getting Started

1. **Examine the tests**: Look at `FileTableUnitTest.java` to understand expected behavior
2. **Implement FileTable.java**: Replace the `throw new UnsupportedOperationException()` statements with your implementation
3. **Run tests**: Use `mvn test` to see which tests pass/fail
4. **Iterate**: Fix failing tests one by one

## Key Requirements

### FileTable Methods to Implement:
- `FileTable(Directory directory)` - Constructor
- `falloc(String fname, String mode)` - Allocate file table entry
- `ffree(FileTableEntry e)` - Free file table entry  
- `fempty()` - Check if table is empty
- `getTable()` - Get the underlying table

### File Access Modes:
- `"r"` - Read mode (inode flag = 1)
- `"w"` - Write mode (inode flag = 2)  
- `"a"` - Append mode (inode flag = 6)

### Special Cases:
- Root directory ("/") always uses inode 0
- Reading non-existent files returns null
- Invalid modes return null
- Append mode sets seekPtr to file length

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=FileTableUnitTest#testFalloc_NewFile_WriteMode

# Compile only
mvn compile
```