# Directory Student Assignment

## Overview
Your task is to implement the `Directory` class that manages file names and their corresponding inode numbers in a flat file system. The directory acts as a mapping between human-readable file names and the internal inode numbers used by the filesystem.

## Project Structure
```
├── lib/ 
│      └── mock.jar # Mock dependencies (SysLib, Constants) 
├── src/ 
│      ├── main/java/ 
│      │            └── Directory.java # YOUR IMPLEMENTATION GOES HERE 
│      └── test/java/ 
│                   └── DirectoryTest.java # Complete test suite 
├── pom.xml # Maven configuration 
└── README.md # This file
```

## Getting Started

1. **Examine the tests**: Look at `DirectoryTest.java` to understand expected behavior
2. **Implement Directory.java**: Replace the `throw new UnsupportedOperationException()` statements with your implementation
3. **Run tests**: Use `mvn test` to see which tests pass/fail
4. **Iterate**: Fix failing tests one by one

## Key Requirements

### Directory Structure
A directory contains:
- `filenameSizes[]` (int array) - Length of each filename (or UNALLOCATED for empty slots)
- `fileNames` (List<String>) - The actual filenames
- `MAX_CHARS` (constant) - Maximum filename length (30 characters)
- `UNALLOCATED` (constant) - Value indicating an unused slot (-1)

### Methods to Implement

#### Core Methods:
- `Directory(int maxInumber)` - Constructor (initialize with root "/" at inode 0)
- `list()` - Return space-separated string of all filenames
- `listFormatted()` - Return list of FilenameLengthPair objects
- `ialloc(String filename)` - Allocate inode number for filename
- `ifree(short iNodeIndex)` - Free an inode number
- `namei(String filename)` - Find inode number for filename

#### Serialization Methods:
- `directory2bytes()` - Convert directory to byte array for disk storage
- `bytes2directory(byte[] data)` - Initialize directory from byte array

### File System Constraints
- **Flat structure only**: No subdirectories or paths like "folder/file"
- **Root directory**: Always at inode 0 with filename "/"
- **Maximum filename length**: 30 characters
- **Maximum files**: 64 total (including root)
- **Case sensitive**: "File" and "file" are different
- **No duplicates**: Cannot allocate same filename twice

### Data Layout on Disk
The directory is serialized as:
```
Bytes 0-255:    File sizes (64 integers × 4 bytes each)
Bytes 256-3839: File names (64 names × 60 bytes each, padded with zeros)
```

## Implementation Tips

### Constructor Logic
```java
public Directory(int maxInumber) {
    filenameSizes = new int[maxInumber];
    fileNames = new ArrayList<>(maxInumber);
    
    // Initialize all entries as unallocated
    for (int i = 0; i < maxInumber; i++) {
        filenameSizes[i] = UNALLOCATED;  // -1
        fileNames.add("");
    }
    
    // Set up root directory at inode 0
    filenameSizes[0] = "/".length();  // 1
    fileNames.set(0, "/");
}
```

### File Allocation Logic
```java
public short ialloc(String filename) {
    // Check if filename already exists
    if (namei(filename) != -1) {
        return -1;  // Already exists
    }
    
    // Check length limit
    if (filename.length() > MAX_CHARS) {
        return -1;  // Too long
    }
    
    // Find first unallocated slot
    for (short i = 1; i < filenameSizes.length; i++) {
        if (filenameSizes[i] == UNALLOCATED) {
            filenameSizes[i] = filename.length();
            fileNames.set(i, filename);
            return i;
        }
    }
    return -1;  // Directory full
}
```

### Name Lookup Logic
```java
public short namei(String filename) {
    for (short i = 0; i < filenameSizes.length; i++) {
        // Only check allocated entries
        if (filenameSizes[i] != UNALLOCATED && 
            filenameSizes[i] == filename.length() && 
            fileNames.get(i).equals(filename)) {
            return i;
        }
    }
    return -1;  // Not found
}
```

### Serialization Logic
```java
public byte[] directory2bytes() {
    byte[] data = new byte[filenameSizes.length * 4 + fileNames.size() * MAX_CHARS * 2];
    int offset = 0;
    
    // Write file sizes
    for (int i = 0; i < filenameSizes.length; i++, offset += 4) {
        SysLib.int2bytes(filenameSizes[i], data, offset);
    }
    
    // Write file names (padded to MAX_CHARS * 2 bytes each)
    for (int i = 0; i < fileNames.size(); i++, offset += MAX_CHARS * 2) {
        String filename = fileNames.get(i);
        byte[] filenameBytes = filename.getBytes();
        
        // Copy filename bytes and pad with zeros
        for (int j = 0; j < MAX_CHARS * 2; j++) {
            if (j < filenameBytes.length) {
                data[offset + j] = filenameBytes[j];
            } else {
                data[offset + j] = 0;  // Padding
            }
        }
    }
    return data;
}
```

## Running Tests

```bash
# Run all tests
mvn test

# Compile only
mvn compile

# Clean and test
mvn clean test
```

## Success Criteria
All 17 tests should pass including:
- Constructor and initialization tests
- File allocation and deallocation tests
- Name lookup and listing tests
- Serialization/deserialization tests
- Edge cases (empty strings, max length, duplicates)
- Error handling (invalid parameters, full directory)

## Common Pitfalls
1. **UNALLOCATED value**: Use -1, not 0 (0 is valid length for empty filenames)
2. **Root directory**: Must always be at inode 0 with filename "/"
3. **Case sensitivity**: "File" ≠ "file"
4. **Bounds checking**: Validate inode numbers and filename lengths
5. **Serialization**: Handle negative sizes properly in bytes2directory()

Good luck with your implementation!