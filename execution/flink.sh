#!/bin/bash

if [[ "$#" != "7" ]]; then
	echo 'expecting 7 arguments:' >&2
	echo '    flink.sh $datasetCategory $datasetName $states $metric $metricArguments $workers $run' >&2
	exit
fi

function getId {
	# 1: type (cc, dd, sssp, tc)
	case $1 in
		"cc")
			echo $ccId
			;;
		"dd")
			echo $ddId
			;;
		"sssp")
			echo $ssspId
			;;
		"tc")
			echo $tcId
			;;
		*)
			echo "invalid metric key: $1" >&2
			exit
			;;
		esac
}

function printTime {
	if [[ -d /Users/benni ]]; then
		gdate +%s%N
	else
		date +%s%N
	fi
}

source config.cfg

datasetCategory=$1
datasetName=$2
states=$3
metric=$4
metricArguments=$5
workers=$6
run=$7


datasetDir="${mainDatasetDir}/$datasetCategory/$datasetName"
runtimesDir="${mainRuntimesDir}/$datasetCategory/$datasetName/$metric--$states"
outputDir="${mainOutputDir}/$datasetCategory/$datasetName/$metric--$states"

if [[ ! -d $runtimesDir ]]; then mkdir -p $runtimesDir; fi
if [[ ! -d outputDir ]]; then mkdir -p $outputDir; fi

runtimes="${runtimesDir}/${run}${runtimesSuffix}"

if [[ -f $runtimes ]]; then echo "$runtimes exists" >&2; exit; fi

for s in $(seq 1 $states); do
	dataset="${datasetDir}/${s}${datasetSuffix}"
	output="${outputDir}/${run}-${s}${outputSuffix}"

	if [[ ! -f $dataset ]]; then echo "$dataset does not exist" >&2; exit; fi

	total_start=$(printTime)
	if [[ $metric == "sssp" ]]; then
		for vertexId in $(echo $metricArguments | tr "," " "); do
			flink run -p $workers $jarPath $dataset $output $(getId $metric) $maxIterations $vertexId
		done
	else
		flink run -p $workers $jarPath $dataset $output $(getId $metric) $maxIterations $metricArguments
	fi
	total_end=$(printTime)
	duration=$((${total_end} - ${total_start}))
	echo "$s	$duration" >> $runtimes
done
echo "TOTAL	$(awk '{ sum += $2; } END { print sum; }' "$runtimes")" >> $runtimes





