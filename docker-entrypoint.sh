#!/bin/sh

echo "Initializing SMHY container..."

# Create log file and output directory
touch /var/log/cron.log
mkdir -p /app/public_html

# Start nginx for serving reports
echo "Starting nginx..."
nginx

# Run forecast once on startup
echo "Running initial forecast on container startup..."
/app/run.sh >> /var/log/cron.log 2>&1 || echo "Initial forecast failed - will retry via cron"

# Create crontab file
echo "55 1-23/3 * * * /app/run.sh >> /var/log/cron.log 2>&1" > /etc/crontabs/root

# Start cron in the background
crond -f -l 2 &

echo "Cron job scheduled: 55 1-23/3 * * * (every 3 hours at 55 minutes past the hour)"
echo "Logs available at: /var/log/cron.log"
echo "Container started successfully"

# Tail the log file to keep container running and show output
tail -f /var/log/cron.log
