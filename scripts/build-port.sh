#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export JAVA_HOME="${JAVA_HOME_21:-/usr/lib/jvm/java-21-openjdk}"
export GRADLE_USER_HOME="$HOME/Downloads/.gradle-dragonminez"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    echo 'Defina JAVA_HOME_21 com o caminho do Java 21.' >&2
    exit 1
fi
./gradlew runData --console=plain
./gradlew build -PoptimizeResources=false --console=plain
