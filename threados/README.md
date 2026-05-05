# ThreadOS Program 5 - File System Integration

## Overview

Program 5 integrates your file system implementation with the complete ThreadOS environment. You will copy your tested core file system components and run comprehensive tests.

## Requirements

- **Java Version**: Java 21 or higher
- **Compatibility**: P5.jar compiled with Java 21 target for maximum compatibility

## Framework Structure

The P5 framework is now properly organized:

- **`lib/P5.jar`**: Contains ALL core ThreadOS components and working filesystem implementations
  - Core FileSystem Files: `FileSystem.class`, `FileTable.class`, `Directory.class`, `SuperBlock.class`, `Inode.class`
  - System Components: `Kernel.class`, `Scheduler.class`, `SysLib.class`, `Disk.class`, `Cache.class`
  - Support Classes: `TCB.class`, `SyncQueue.class`, `QueueNode.class`, `FileTableEntry.class`, `Constants.class`

- **`src/`**: Contains only the files students need to see and potentially modify
  - `Boot.java`: ThreadOS boot loader
  - `Loader.java`: Thread loader implementation  
  - `Test5.java`: Comprehensive file system integration tests
  - `Test6.java`: Additional concurrent access tests
  - `HelloWorld.java`: Simple test program

**Key Advantage**: Students can copy their core filesystem implementations to `src/` and they will take precedence over the versions in `P5.jar`.

## Instructions

### Option 1: Test Individual Files (Recommended)
1. **Replace Individual Files**: Copy your tested implementations one at a time to replace the working versions in `src/`:
   - Replace `FileSystem.java` with your implementation
   - Replace `FileTable.java` with your implementation  
   - Replace `Directory.java` with your implementation
   - Replace `SuperBlock.java` with your implementation
   - Replace `Inode.java` with your implementation

2. **Test Incrementally**: After replacing each file, compile and test:
   ```bash
   javac --release 21 -cp lib/P5.jar src/*.java
   java -cp lib/P5.jar:src Boot
   ```

### Option 2: Test All Files Together
1. **Replace All Core Files**: Copy all your tested implementations at once
2. **Compile and Test**: 
   ```bash
   javac --release 21 -cp lib/P5.jar src/*.java
   java -cp lib/P5.jar:src Boot
   ```

### Option 3: Use the Run Script
```bash
./run.sh
```

## Notes

- The DISK file will be created automatically when ThreadOS runs
- **All files in `src/` are working, tested implementations from V2.2**
- Files in `src/` override any versions compiled in `P5.jar`
- Replace individual files with your implementations to test incrementally
- Your core file system files should be the same ones that passed all unit tests
- If your implementation has issues, you can revert to the working version

## Testing

The Test5 program will verify:
- File creation and deletion
- Directory operations
- File I/O operations
- Concurrent file access
- File system integrity

Make sure all your unit tests pass before copying files to this directory.