#!/usr/bin/env bash
set -u

echo "ENTER_PROBE_READY os=posix pid=$$"

input=""
while IFS= read -r -n1 ch; do
  ord=$(printf '%d' "'$ch")
  hex=$(printf '%02X' "$ord")
  if [ "$ord" -eq 13 ] || [ "$ord" -eq 10 ]; then
    echo "ENTER_PROBE_SIGNAL byte=$hex len=${#input}"
    printf 'ENTER_PROBE_BUFFER escaped=%q\n' "$input"
    input=""
  else
    input+="$ch"
  fi
done

echo "ENTER_PROBE_EOF"
