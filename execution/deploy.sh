#!/bin/bash

source jobs.cfg

rsync -auvzl \
	../install_flink.sh ../install_gelly_streaming.sh ../uninstall.sh \
	../start.sh ../stop.sh \
	../pom.xml ../src \
	flink.sh config.cfg \
	datasets \
	$server_name:$server_dir/