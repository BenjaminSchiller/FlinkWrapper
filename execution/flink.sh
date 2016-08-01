#!/bin/bash

if [[ "$#" != "6" ]]; then
	echo 'expecting 6 arguments:' >&2
	echo '    flink.sh $dataset $states $metric $metricArguments $workers $run' >&2
	exit
fi

function printTime {
	if [[ -d /Users/benni ]]; then
		gdate +%s%N
	else
		date +%s%N
	fi
}

source config.cfg

dataset=$1
states=$2
metric=$3
metricArguments=$4
workers=$5
run=$6

# 1: type (cc, dd, sssp, tc)
case $metric in
	"cc")
		metricId=$ccId
		;;
	"dd")
		metricId=$ddId
		;;
	"sssp")
		metricId=$ssspId
		;;
	"tc")
		metricId=$tcId
		;;
	*)
		echo "invalid metric key: $metric" >&2
		exit
		;;
	esac


datasetDir="${mainDatasetDir}/${dataset}"
runtimesDir="${mainRuntimesDir}/$dataset/$states/$metric/$workers"
logDir="${mainLogDir}/$dataset/$states/$metric/$workers"
outputDir="${mainOutputDir}/$dataset/$states/$metric/$workers"

if [[ ! -d $runtimesDir ]]; then mkdir -p $runtimesDir; fi
if [[ ! -d $logDir ]]; then mkdir -p $logDir; fi
if [[ ! -d outputDir ]]; then mkdir -p $outputDir; fi

runtimes="${runtimesDir}/${run}${runtimesSuffix}"

if [[ -f $runtimes ]]; then echo "$runtimes exists" >&2; exit; fi

for s in $(seq 0 $((states-1))); do
	datasetPath="${datasetDir}/${s}${datasetSuffix}"

	if [[ ! -f $datasetPath ]]; then echo "$datasetPath does not exist" >&2; exit; fi

	total_start=$(printTime)
	if [[ $metric == "sssp" ]]; then
		for vertexId in $(echo $metricArguments | tr "," " "); do
			log="${logDir}/${run}-${s}--${vertexId}${logSuffix}"
			err="${logDir}/${run}-${s}--${vertexId}${errSuffix}"
			output="${outputDir}/${run}-${s}--${vertexId}${outputSuffix}"
			flink run -p $workers $jarPath $datasetPath $output $metricId $maxIterations $vertexId > >(tee $log) 2> >(tee $err >&2)
		done
	else
		log="${logDir}/${run}-${s}${logSuffix}"
		err="${logDir}/${run}-${s}${errSuffix}"
		output="${outputDir}/${run}-${s}${outputSuffix}"
		flink run -p $workers $jarPath $datasetPath $output $metricId $maxIterations $metricArguments > >(tee $log) 2> >(tee $err >&2)
	fi
	total_end=$(printTime)
	duration=$((${total_end} - ${total_start}))
	echo "$s	$duration" >> $runtimes
done
echo "TOTAL	$(awk '{ sum += $2; } END { print sum; }' "$runtimes")" >> $runtimes





