# ThreadOS File System

A Unix-like file system implementation built for the ThreadOS operating system simulator. This project enables persistent file storage through a structured system of inodes, file descriptors, and disk block management.

## Overview

This file system replaces raw disk access with a higher-level abstraction that allows user programs to interact with files using standard operations such as open, read, write, seek, close, and delete. It uses a single-level root directory and supports multiple concurrent file accesses through per-thread file descriptor tables.

## Features

- Full file lifecycle support: format, open, read, write, seek, close, delete, fsize
- Inode-based storage with direct and indirect block addressing
- Per-thread file descriptor tables (32 entries per thread)
- Shared file table for managing concurrent access
- Seek pointer management with SEEK_SET, SEEK_CUR, SEEK_END behavior
- Support for read, write, read/write, and append modes
- Free block list management for efficient disk allocation
- Persistent disk storage using ThreadOS disk simulation

## Tech Stack

- Java
- ThreadOS operating system simulator
- Maven
- JUnit

## Project Structure

P5/
  lib/
    P5.jar
  src/
    Boot.java
    Directory.java
    FileSystem.java
    FileTable.java
    Inode.java
    SuperBlock.java
    Test5.java
    Test6.java
  run.sh
  test_framework.sh

UnitTests/
  P5/
    Directory/
    FileSystem/
    FileTable/
    Inode/
    SuperBlock/

output.txt

## How to Run

cd P5
./run.sh

Inside the ThreadOS shell:

l Test5

## Testing

All subsystems are validated using unit tests and ThreadOS integration tests. The project passes:

- Directory tests
- SuperBlock tests
- Inode tests
- FileTable tests
- FileSystem tests
- ThreadOS Test5 integration test

## Results

The file system correctly handles:

- File creation and deletion
- Sequential and random access reads and writes
- Appending data to files
- Seeking within small and large files
- Handling multiple file descriptors referencing the same file
- Creating and managing large numbers of files

## Key Concepts Demonstrated

- Inode-based file system design
- Disk block allocation and free list management
- File descriptor abstraction and per-thread state
- Seek pointer management and file offsets
- Separation of system-wide and per-thread file state
- Test-driven development for low-level systems
