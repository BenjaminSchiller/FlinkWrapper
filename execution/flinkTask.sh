#!/bin/bash

if [[ "$#" != "6" ]]; then
	echo 'expecting 6 arguments:' >&2
	echo '    flinkTask.sh $dataset $states $metric $metricArguments $workers $run' >&2
	exit
fi

source config.cfg

dataset=$1
states=$2
metric=$3
metricArguments=$4
workers=$5
run=$6


endCpu=$(($startCpu+$auxCpus+$workers-1))
echo "binding flink to cpus: $startCpu-$endCpu / $startCpu+$auxCpus+$workers-1"
taskset -c "${startCpu}-${endCpu}" ./flink.sh $dataset $states $metric $metricArguments $workers $run
echo "DONE"

