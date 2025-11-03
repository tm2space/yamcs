#!/bin/bash

# Set JAVA_HOME based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    export JAVA_HOME=$(/usr/libexec/java_home -v 17)
else
    # Linux - auto-detect Java 17 or use system default
    if [ -z "$JAVA_HOME" ]; then
        # Try to find Java 17
        if command -v java >/dev/null 2>&1; then
            JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "?\K[0-9]+')
            if [ "$JAVA_VERSION" -ge 17 ]; then
                echo "Using system Java version $JAVA_VERSION"
            else
                echo "Warning: Java 17+ required, found version $JAVA_VERSION"
            fi
        else
            echo "Error: Java not found. Please install Java 17 or later."
            exit 1
        fi
    fi
fi

cd yamcs-web/src/main/webapp && npm run build && cd - && ./run-example.sh simulation