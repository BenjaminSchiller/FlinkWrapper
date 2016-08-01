#!/bin/bash

echo "$(date) - stopping flink job manager"
ps aux | grep flink | grep -v grep | grep JobManager | sudo kill $(awk {'print $2'})
echo "$(date) - stopped flink job manager"
