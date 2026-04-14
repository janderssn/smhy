#!/bin/sh

# SMHY execution script
# Runs the application with --all and --heartbeat flags

set -e

cd /app

echo "$(date) - Starting SMHY execution"

# Run the application with all flags enabled and heartbeat
java -Dspring.profiles.active=prod -jar lib/console-0.0.1-SNAPSHOT.jar --all --heartbeat

echo "$(date) - SMHY execution completed"
