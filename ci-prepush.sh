#!/bin/bash

# CI Pre-push Script for KMP Library
# Runs all checks and tests to ensure code quality before pushing

# Check if gradlew exists in the project
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found in the project."
    exit 1
fi

echo "Starting all checks and tests..."

failed_tasks=()
successful_tasks=()

run_gradle_task() {
    echo ""
    echo "========================================"
    echo "Running: $1"
    echo "========================================"
    "./gradlew" $1
    if [ $? -ne 0 ]; then
        echo "Warning: Task $1 failed"
        failed_tasks+=("$1")
    else
        echo "Task $1 completed successfully"
        successful_tasks+=("$1")
    fi
}

tasks=(
    # Code formatting
    "spotlessApply"

    # Static analysis
    "detekt"

    # JVM tests
    "jvmTest"

    # Linux tests (only on Linux)
    # "linuxX64Test"

    # Build all targets to verify compilation
    "assemble"
)

for task in "${tasks[@]}"; do
    run_gradle_task "$task"
done

echo ""
echo "========================================"
echo "Summary"
echo "========================================"

echo ""
echo "Successful tasks:"
for task in "${successful_tasks[@]}"; do
    echo "  ✓ $task"
done

if [ ${#failed_tasks[@]} -eq 0 ]; then
    echo ""
    echo "All checks and tests completed successfully!"
    echo "You're good to push."
    exit 0
else
    echo ""
    echo "Failed tasks:"
    for task in "${failed_tasks[@]}"; do
        echo "  ✗ $task"
    done
    echo ""
    echo "Please review the output above for more details on the failures."
    exit 1
fi
