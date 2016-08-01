#!/bin/bash

ps aux | grep flink | grep -v grep | grep JobManager | sudo kill $(awk {'print $2'})
