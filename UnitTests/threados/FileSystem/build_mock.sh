#!/bin/bash

echo "=========================================="
echo "FileSystem Isolated Unit Test Mock JAR Builder"
echo "Version 1.0"
echo "=========================================="

# Define the dependencies needed for FileSystem isolated unit test
# NOTE: FileSystem.class is NOT included as it's the class being tested
DEPENDENCIES=(
    "Constants.class"
    "Directory.class"
    "Directory\$FilenameLengthPair.class"
    "Disk.class"
    "FileTable.class"
    "FileTableEntry.class"
    "Inode.class"
    "LoggingSetup.class"
    "SuperBlock.class"
    "SysLib.class"
)

# Source directory for compiled classes
SOURCE_DIR="target/classes"

# Create lib directory if it doesn't exist
mkdir -p lib

# Create temporary directory for building JAR
TEMP_DIR=$(mktemp -d)
echo "Using temporary directory: $TEMP_DIR"

echo ""
echo "Building mock.jar for FileSystem isolated unit test..."
echo "Dependencies: ${DEPENDENCIES[*]}"

# Copy dependency class files
echo "Copying dependency class files..."
for dep in "${DEPENDENCIES[@]}"; do
    if [ -f "$SOURCE_DIR/$dep" ]; then
        cp "$SOURCE_DIR/$dep" "$TEMP_DIR/"
        echo "  ✓ Copied $dep"
    else
        echo "  ❌ Missing $dep"
        exit 1
    fi
done

# Create the JAR file
echo "Creating mock.jar..."
cd "$TEMP_DIR"
jar cf ../lib/mock.jar *.class
cd - > /dev/null

# Verify the JAR contents
echo "Verifying mock.jar contents..."
jar -tf lib/mock.jar | sort

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo "✅ mock.jar created successfully for FileSystem isolated unit test"
echo "Location: lib/mock.jar"
echo "Dependencies included: ${#DEPENDENCIES[@]} dependency files (FileSystem.class excluded as it's the class being tested)"