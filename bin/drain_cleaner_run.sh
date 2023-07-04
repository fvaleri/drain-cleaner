#!/usr/bin/env bash
set -e
set +x

# Exit when we run out of heap memory
JAVA_OPTS="${JAVA_OPTS} -XX:+ExitOnOutOfMemoryError"

export JAVA_CLASSPATH="$JAVA_CLASSPATH:lib/*:app.jar"
export JAVA_MAIN="io.strimzi.Main"

# Disable FIPS if needed
if [[ "$FIPS_MODE" = "disabled" ]]; then
  export JAVA_OPTS="${JAVA_OPTS} -Dcom.redhat.fips=false"
fi

# Start Drain Cleaner
# shellcheck disable=SC2086
exec /usr/bin/tini -s -w -e 143 -- java $JAVA_OPTS -cp "$JAVA_CLASSPATH" "$JAVA_MAIN" "$@"
