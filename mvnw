#!/bin/sh
# Maven executable script for civic-reporting
MVN_BIN="/home/abhinav/.m2/wrapper/dists/apache-maven-3.6.3-bin/1iopthnavndlasol9gbrbg6bf2/apache-maven-3.6.3/bin/mvn"

if [ -x "$MVN_BIN" ]; then
    exec "$MVN_BIN" "$@"
elif command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
else
    echo "Error: Maven not found." >&2
    exit 1
fi
