#!/usr/bin/env bash

TEST_DIR="testcases"
PASSED=0
TOTAL=0

for file in "$TEST_DIR"/scramble*; do
    [ -e "$file" ] || continue
    TOTAL=$((TOTAL + 1))

    echo "=== Running test: $file ==="
    
    OUTPUT=$(java -cp out rubikscube.Solver "$file" solution01.txt)

    # If output contains "Steps limit reached" → failed
    echo "$OUTPUT" | grep -q "Steps limit reached"
    if [ $? -eq 0 ]; then
        echo "❌ FAILED: $file"
        echo
        continue
    fi

    # Extract "Solution found: XXXXXXX in NN steps"
    SOLUTION=$(echo "$OUTPUT" | grep -oE "Solution found: [A-Za-z]+ in [0-9]+ steps")
    if [ -n "$SOLUTION" ]; then
        PASSED=$((PASSED + 1))
        echo "✅ PASSED: $file"
        echo "👉 $SOLUTION"
    else
        echo "⚠️ UNKNOWN RESULT FOR: $file"
    fi
    echo
done

echo "--------------------------------"
echo "Tests passed: $PASSED / $TOTAL"
echo "--------------------------------"
