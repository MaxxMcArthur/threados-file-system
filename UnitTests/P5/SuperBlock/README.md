# SuperBlock Student Assignment

## Overview
Your task is to implement the `SuperBlock` class that manages filesystem metadata and free block allocation. The superblock is the control structure that tracks the overall state of the filesystem, including the total number of blocks, inode allocation, and the free block list.

## Project Structure
```
├── lib/ 
│      └── mock.jar # Mock dependencies (SysLib, Constants, Disk, Inode) 
├── src/ 
│      ├── main/java/ 
│      │            └── SuperBlock.java # YOUR IMPLEMENTATION GOES HERE 
│      └── test/java/ 
│                   └── SuperBlockTest.java # Complete test suite 
├── pom.xml # Maven configuration 
└── README.md # This file
```

## Getting Started

1. **Examine the tests**: Look at `SuperBlockTest.java` to understand expected behavior
2. **Implement SuperBlock.java**: Replace the `throw new UnsupportedOperationException()` statements with your implementation
3. **Run tests**: Use `mvn test` to see which tests pass/fail
4. **Iterate**: Fix failing tests one by one

## Key Requirements

### SuperBlock Structure
A superblock contains the following fields:
- `totalBlocks` (int) - Total number of blocks in the filesystem
- `inodeBlocks` (int) - Number of blocks allocated for inodes
- `freeList` (int) - Block number of the first free block in the linked list
- `defaultInodeBlocks` (int) - Default number of inode blocks (typically 64)

### Methods to Implement

#### Core Methods:
- `SuperBlock(int diskBlocks)` - Constructor (read from disk or format if invalid)
- `sync()` - Write superblock data to disk
- `format()` - Format filesystem with default inode blocks
- `format(int inodeBlocks)` - Format filesystem with specified inode blocks

#### Free Block Management:
- `getFreeBlock()` - Allocate a free block from the free list
- `returnBlock(int oldBlockNumber)` - Return a block to the free list

### Disk Layout
The superblock is stored at block 0 and contains:
Offset Size Field 0 4 totalBlocks (int) 4 4 inodeBlocks (int)
8 4 freeList (int) 12 ... (unused space)


### Free Block Management
The filesystem uses a linked list to track free blocks:
- Each free block contains the block number of the next free block
- The superblock's `freeList` field points to the first free block
- The last free block contains 0 (end of list)
- When allocating: remove from head of list
- When deallocating: add to head of list

### Filesystem Layout
Block 0: Superblock Block 1-N: Inode blocks (N = inodeBlocks) Block N+1: Start of free list and data blocks


## Implementation Tips

### Constructor Logic
```java
public SuperBlock(int diskBlocks) {
    // Read superblock from disk
    byte[] superBlock = new byte[Disk.blockSize];
    SysLib.rawread(Constants.SUPERBLOCK_BLOCK, superBlock);
    
    // Extract fields
    totalBlocks = SysLib.bytes2int(superBlock, 0);
    inodeBlocks = SysLib.bytes2int(superBlock, 4);
    freeList = SysLib.bytes2int(superBlock, 8);
    
    // Validate data - if invalid, format
    if (/* validation fails */) {
        totalBlocks = diskBlocks;
        format(defaultInodeBlocks);
    }
}
Free Block Allocation
public int getFreeBlock() {
    int freeBlockNumber = freeList;
    
    if (freeBlockNumber != -1) {
        // Read the free block to get next pointer
        byte[] newBlock = new byte[Disk.blockSize];
        SysLib.rawread(freeBlockNumber, newBlock);
        
        // Update free list to point to next free block
        freeList = SysLib.bytes2int(newBlock, 0);
        
        // Clear the allocated block
        SysLib.int2bytes(0, newBlock, 0);
        SysLib.rawwrite(freeBlockNumber, newBlock);
    }
    
    return freeBlockNumber;
}
Running Tests
# Run all tests
mvn test

# Compile only
mvn compile
Success Criteria
All 14 tests should pass including:

Constructor and validation tests
Sync and format tests
Free block allocation/deallocation tests
Free list management tests
Good luck with your implementation! EOF

echo "Created README.md for SuperBlock student assignment"
