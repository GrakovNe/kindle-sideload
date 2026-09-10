#!/usr/bin/env bash
#
# Starts kindle-sideload with a Project Leyden AOT cache (JDK 25+).
#
# The cache is bound to the exact JDK build, OS/arch and classpath, so it has to be
# produced on the machine that runs the bot. This script does that on demand: when the
# jar is newer than the cache (fresh deploy) or the cache is missing, it re-extracts the
# jar, runs a short training pass (Spring context up, then exit) and only then starts
# the application with the cache attached.
#
# Usage:
#   tools/leyden-run.sh [path/to/app.jar]        # default: ./app.jar
#   TRAIN_ONLY=1 tools/leyden-run.sh app.jar     # only (re)build the cache, do not start
#                                                 (safe next to a running instance: fake
#                                                  token, random port; use it in deploy
#                                                  before restarting the service)
#
# Environment:
#   JAVA_HOME     JDK 25+ to use (falls back to `java` on PATH)
#   APP_DIR       where the extracted jar and the cache live (default: ./leyden)
#   JAVA_OPTS     extra flags appended to the production run
#   HEAP_MAX      -Xmx for the production run (default: 512m)
#
# Example systemd unit:
#   [Service]
#   WorkingDirectory=/opt/kindle-sideload
#   ExecStart=/opt/kindle-sideload/tools/leyden-run.sh /opt/kindle-sideload/app.jar
#   Restart=always
#
# The working directory matters: the app reads ./locale, ./user_configurations,
# ./fb2_converter and ./temporary_user_environments relative to it, and the training
# pass must see the same layout as the production run.

set -euo pipefail

JAR="${1:-app.jar}"
APP_DIR="${APP_DIR:-leyden}"
HEAP_MAX="${HEAP_MAX:-512m}"

if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi

if [[ ! -f "$JAR" ]]; then
  echo "leyden-run: jar not found: $JAR" >&2
  exit 1
fi

JAVA_MAJOR="$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.specification.version = //p')"
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 25 ]]; then
  echo "leyden-run: JDK 25+ required, found '${JAVA_MAJOR:-unknown}' ($JAVA)" >&2
  exit 1
fi

JAR_NAME="$(basename "$JAR")"
EXTRACTED="$APP_DIR/$JAR_NAME"
CACHE="$APP_DIR/app.aot"

# ---------------------------------------------------------------------------
# 1. (Re)build the cache when the jar changed or the cache is missing
# ---------------------------------------------------------------------------
if [[ ! -f "$CACHE" || ! -f "$EXTRACTED" || "$JAR" -nt "$CACHE" ]]; then
  echo "leyden-run: (re)building AOT cache for $JAR"
  rm -rf "$APP_DIR"
  "$JAVA" -Djarmode=tools -jar "$JAR" extract --destination "$APP_DIR"

  # Training pass. The context is fully refreshed (beans, Flyway, jOOQ, Thymeleaf, bot
  # wiring) and the JVM exits right after. The Telegram token is overridden so the
  # training process can never consume real updates, and the web port is random so
  # it does not clash with a still-running instance.
  "$JAVA" \
    -XX:AOTCacheOutput="$CACHE" \
    -Xlog:aot=error \
    -Dspring.context.exit=onRefresh \
    -Dtelegram.token=aot-training \
    -Dserver.port=0 \
    -jar "$EXTRACTED"

  echo "leyden-run: cache written: $(du -h "$CACHE" | cut -f1)"
elif [[ "${TRAIN_ONLY:-0}" == "1" ]]; then
  echo "leyden-run: cache is up to date: $CACHE"
fi

if [[ "${TRAIN_ONLY:-0}" == "1" ]]; then
  exit 0
fi

# ---------------------------------------------------------------------------
# 2. Production run
# ---------------------------------------------------------------------------
# -XX:AOTCache                 Leyden: pre-loaded/linked classes + method profiles
# -XX:+ExitOnOutOfMemoryError  let systemd restart a wedged process instead of limping
# -XX:+UseStringDeduplication  message/template strings are highly repetitive
# -Xss512k                     the bot is thread-light; smaller stacks trim RSS
exec "$JAVA" \
  -XX:AOTCache="$CACHE" \
  -Xlog:aot=error \
  -Xms128m -Xmx"$HEAP_MAX" \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XX:+ExitOnOutOfMemoryError \
  -Xss512k \
  -Dfile.encoding=UTF-8 \
  ${JAVA_OPTS:-} \
  -jar "$EXTRACTED"
