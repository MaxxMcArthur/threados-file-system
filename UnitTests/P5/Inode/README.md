# Inode Student Assignment

## Overview
Your task is to implement the `Inode` class that represents file metadata in a filesystem. An inode (index node) stores essential information about a file including its size, reference count, status flags, and pointers to data blocks.

## Project Structure
```
├── lib/
│   └── mock.jar              # Mock dependencies (SysLib, Constants, Disk)
├── src/
│   ├── main/java/
│   │   └── Inode.java        # YOUR IMPLEMENTATION GOES HERE
│   └── test/java/
│       └── InodeTest.java    # Complete test suite
├── pom.xml                   # Maven configuration
└── README.md                 # This file
```

## Getting Started

1. **Examine the tests**: Look at `InodeTest.java` to understand expected behavior
2. **Implement Inode.java**: Replace the `throw new UnsupportedOperationException()` statements with your implementation
3. **Run tests**: Use `mvn test` to see which tests pass/fail
4. **Iterate**: Fix failing tests one by one

## Key Requirements

### Inode Structure
An inode contains the following fields:
- `length` (int) - File size in bytes
- `count` (short) - Number of file table entries pointing to this inode
- `flag` (short) - Status flag (0=unused, 1=read, 2=write, etc.)
- `direct[]` (short array) - Direct pointers to data blocks (size = Constants.DIRECT_SIZE = 11)
- `indirect` (short) - Pointer to indirect block for larger files

### Methods to Implement

#### Core Methods:
- `Inode()` - Default constructor (initialize empty inode)
- `Inode(short iNumber)` - Load inode from disk
- `toDisk(short iNumber)` - Write inode to disk
- `findTargetBlock(int offset)` - Find block containing data at offset
- `findIndexBlock()` - Get indirect block number

#### Advanced Methods:
- `registerTargetBlock(int offset, short targetBlockNumber)` - Register a block for an offset
- `registerIndexBlock(short indexBlockNumber)` - Set up indirect block
- `unregisterIndexBlock()` - Remove indirect block and return its contents
- `printInode()` - Debug output

### Disk Layout
Inodes are stored on disk starting at block 1. Each block can hold multiple inodes:
- Block number = `1 + iNumber / iNodesPerBlock`
- Offset within block = `(iNumber % iNodesPerBlock) * iNodeSize`

### Inode Data Layout (32 bytes total):
```
Offset  Size  Field
0       4     length (int)
4       2     count (short)
6       2     flag (short)
8       22    direct[11] (11 shorts, 2 bytes each)
30      2     indirect (short)
```

### Block Addressing
- **Direct blocks**: For small files, use `direct[]` array
  - Block index = `offset / Disk.blockSize`
  - Can address up to `directSize * blockSize` bytes
- **Indirect blocks**: For larger files, use indirect pointer
  - Indirect block contains array of block numbers
  - Each entry is a short (2 bytes)
  - Can hold `Disk.blockSize / 2` block numbers

### Error Codes
- `NoError = 0` - Success
- `ErrorBlockRegistered = -1` - Block already registered
- `ErrorPrecBlockUnused = -2` - Previous block not assigned
- `ErrorIndirectNull = -3` - Indirect pointer not set
- `UNASSIGNED = -1` - Unassigned block pointer

## Implementation Tips

### Constructor (Default)
```java
public Inode() {
    length = 0;
    count = 0;
    flag = 0;
    direct = new short[directSize];
    for (int i = 0; i < directSize; i++) {
        direct[i] = UNASSIGNED;
    }
    indirect = UNASSIGNED;
}
```

### Reading from Disk
```java
// Calculate block and offset
int blockNumber = 1 + iNumber / iNodesPerBlock;
int offset = (iNumber % iNodesPerBlock) * iNodeSize;

// Read block
byte[] data = new byte[Disk.blockSize];
SysLib.rawread(blockNumber, data);

// Extract fields
length = SysLib.bytes2int(data, offset);
count = SysLib.bytes2short(data, offset + 4);
// ... continue for other fields
```

### Finding Target Block
```java
if (offset < directSize * Disk.blockSize) {
    // Use direct pointer
    int directIndex = offset / Disk.blockSize;
    return direct[directIndex];
} else {
    // Use indirect pointer
    if (indirect != UNASSIGNED) {
        // Read indirect block and find target
        // ...
    }
}
```

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=InodeTest#testInodeConstructor_NewInode

# Compile only
mvn compile

# Clean and rebuild
mvn clean compile test
```

## Test Categories

1. **Constructor Tests**: Verify proper initialization
2. **Disk I/O Tests**: Test reading/writing to disk
3. **Block Finding Tests**: Test findTargetBlock logic
4. **Index Block Tests**: Test indirect block management
5. **Debug Tests**: Test printInode output

## Common Pitfalls

1. **Byte Order**: Use SysLib methods for byte conversion
2. **Block Calculations**: Remember inodes start at block 1, not 0
3. **Array Bounds**: Check directSize and indirect block limits
4. **Error Handling**: Return proper error codes
5. **Disk Synchronization**: Always call toDisk() after modifications

## Success Criteria

All 14 tests should pass:
- ✅ testInodeConstants
- ✅ testInodeConstructor_NewInode
- ✅ testInodeConstructor_FromDisk
- ✅ testToDisk
- ✅ testFindTargetBlock
- ✅ testFindIndexBlock
- ✅ testUnregisterIndexBlock_IndirectBlockAssigned
- ✅ testUnregisterIndexBlock_NoIndirectBlockAssigned
- ✅ testUnregisterIndexBlock_IndirectBlockAssigned_All_NegOne
- ✅ testPrintDefault
- ✅ testPrintWithData

## Debugging

Use the `printInode()` method to debug your implementation:
```java
inode.printInode();
```

This should output:
```
Inode Debug Information:
  Length: 1024
  Count: 2
  Flag: 1
  Direct Pointers:
    direct[0]: 100
    direct[1]: 200
    ...
  Indirect Pointer: 300
End Inode Debug Information.
```

## Resources

- Review the test cases to understand expected behavior
- Use `SysLib.cerr()` for debug output
- Check Constants.java for important values
- Remember: block size is 512 bytes (Disk.blockSize)

Good luck with your implementation!