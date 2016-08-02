#!/bin/bash

function sparkJob {
	# $1: dataset
	# $2: states
	# $3: metric
	# $4: metricArguments
	# $5: workers
	# $6: run
	./jobs.sh create "./flinkTask.sh $1 $2 $3 $4 $5 $6"
}


dataset="Undirected__-/Random__10000_50000/Random__0_0_0_0/0__0"
states="1"


dataset="Undirected__-/Random__100_500/RandomEdgeExchange__10_99999999/0__99"
states="19"
workerss=(1 2)
runs=(4)


for run in ${runs[@]}; do
	for workers in ${workerss[@]}; do
		sparkJob $dataset $states dd 0 $workers $run
	done
done
