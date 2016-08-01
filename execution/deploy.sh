#!/bin/bash

source jobs.cfg

rsync -auvzl \
	../install_flink.sh ../install_gelly_streaming.sh ../uninstall.sh \
	../start-job-manager.sh ../stop-job-manager.sh \
	../pom.xml ../src \
	flink.sh config.cfg \
	clean.sh \
	$server_name:$server_dir/