#!/bin/bash

echo "=========================================="
echo "ThreadOS P5 Framework Test"
echo "=========================================="

# Clean up any previous compilation
echo "Cleaning previous compilation..."
rm -f src/*.class

# Compile all source files
echo "Compiling source files..."
javac --release 17 -cp lib/P5.jar src/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Framework is ready for student use."
    echo ""
    echo "Students can now:"
    echo "1. Replace individual core files (FileSystem.java, FileTable.java, etc.)"
    echo "2. Compile with: javac --release 17 -cp lib/P5.jar src/*.java"
    echo "3. Run tests with: java -cp lib/P5.jar:src Boot"
    echo ""
    echo "All files in src/ take precedence over P5.jar versions."
else
    echo "❌ Compilation failed!"
    echo "Check for missing dependencies or syntax errors."
    exit 1
fi

echo "=========================================="
echo "Test completed successfully!"
echo "=========================================="