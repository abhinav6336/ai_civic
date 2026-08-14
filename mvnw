#!/bin/sh
# Maven wrapper wrapper script for local environment
DIR="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${DIR}/../.tools/jdk-21.0.2+13"
export PATH="${JAVA_HOME}/bin:${DIR}/../.tools/apache-maven-3.9.6/bin:${PATH}"

exec "${DIR}/../.tools/apache-maven-3.9.6/bin/mvn" "$@"
