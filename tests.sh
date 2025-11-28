#!/usr/bin/env bash

TEST_DIR="testcases"
PASSED=0
TOTAL=0

# 如果有参数，则只跑指定测试编号
if [ $# -eq 1 ]; then
    # 补零：1 → 01, 2 → 02
    TEST_NUM=$(printf "%02d" "$1")
    FILE="$TEST_DIR/scramble$TEST_NUM.txt"
    if [ ! -e "$FILE" ]; then
        echo "Test file not found: $FILE"
        exit 1
    fi
    FILES=("$FILE")
else
    FILES=("$TEST_DIR"/scramble*)
fi

for file in "${FILES[@]}"; do
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

    # Extract Time taken
    TIME=$(echo "$OUTPUT" | grep -oE "Time taken: [0-9]+ milliseconds")
    if [ -n "$TIME" ]; then
        echo "⏱ $TIME"
    fi

    echo
done

echo "--------------------------------"
echo "Tests passed: $PASSED / $TOTAL"
echo "--------------------------------"
