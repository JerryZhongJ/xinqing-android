#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Android's org.json classes are SDK stubs; JVM tests use the pinned JSON-java runtime.
json_jar="${JSON_TEST_JAR:-build/json-test.jar}"
if [[ ! -f "$json_jar" ]]; then
    mkdir -p "$(dirname "$json_jar")"
    curl --fail --location --connect-timeout 15 --max-time 60 \
        https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar \
        --output "$json_jar"
fi
if [[ -z "${JSON_TEST_JAR:-}" ]]; then
    printf '%s  %s\n' 3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed "$json_jar" | sha256sum --check --status
fi
mkdir -p build/test-classes
javac -encoding UTF-8 -cp "$json_jar" -d build/test-classes \
    src/cn/xinqing/journal/{ThoughtMethods,MethodRecords,CbtReferences,AiPrompts,AiSuggestions}.java \
    tests/*.java tools/ExportPrompts.java
for suite in AiSuggestionsTest PromptGeneralityTest MethodRecordsTest; do
    java -cp "$json_jar:build/test-classes" "cn.xinqing.journal.$suite"
done
