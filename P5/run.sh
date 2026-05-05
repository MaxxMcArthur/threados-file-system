#!/bin/bash

echo "=========================================="
echo "ThreadOS P5 - Starting ThreadOS"
echo "=========================================="

# Check if P5.jar exists
if [ ! -f "lib/P5.jar" ]; then
    echo "❌ Error: lib/P5.jar not found!"
    exit 1
fi

# Compile source files if needed
if [ ! -f "src/Boot.class" ] || [ ! -f "src/Test5.class" ]; then
    echo "Compiling source files..."
    javac --release 17 -cp lib/P5.jar src/*.java
    if [ $? -ne 0 ]; then
        echo "❌ Compilation failed!"
        exit 1
    fi
fi

echo "Starting ThreadOS with:"
echo "  - P5.jar: All core system components and filesystem implementations"
echo "  - src/: Boot, Loader, Test5, Test6, HelloWorld"
echo ""

# Run ThreadOS
# java -cp lib/P5.jar:src Boot
java -cp src:lib/P5.jar Boot

echo ""
echo "=========================================="
echo "ThreadOS execution completed"
echo "=========================================="