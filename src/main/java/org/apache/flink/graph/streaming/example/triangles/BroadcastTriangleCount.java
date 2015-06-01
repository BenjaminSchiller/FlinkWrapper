package org.apache.flink.graph.streaming.example.triangles;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.streaming.example.triangles.util.TriangleEstimate;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.NullValue;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastTriangleCount {

	public BroadcastTriangleCount() throws Exception {

		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();

		// A random graph I generated, details:
		//   - 100 vertices
		//   - 954 edges
		//   - 884 triangles
		DataStream<Edge<Long, NullValue>> edges = env.readTextFile("big_random_graph.txt")
				.flatMap(new FlatMapFunction<String, Edge<Long, NullValue>>() {
					@Override
					public void flatMap(String s, Collector<Edge<Long, NullValue>> out) throws Exception {
						// Parse lines from the text file
						String[] args = s.split(" ");
						long src = Long.parseLong(args[0]);
						long trg = Long.parseLong(args[1]);

						out.collect(new Edge<>(src, trg, NullValue.getInstance()));
					}
				});

		final int p = env.getParallelism();
		final int totalSize = 20000;
		final int instanceSize = totalSize / p;

		DataStream<TriangleEstimate> results = edges
				.broadcast()
				.flatMap(new TriangleSampler(instanceSize));

		// Extract deltaBeta and sequence number
		results.flatMap(new TriangleSummer(totalSize)).setParallelism(1)
				.print().setParallelism(1);

		// The output format is <edge count, triangle estimate>
		JobExecutionResult res = env.execute("Streaming Triangle Count (Estimate)");
		System.out.println("Runtime: " + res.getNetRuntime());
	}

	private static final class TriangleSampler extends RichFlatMapFunction<Edge<Long, NullValue>, TriangleEstimate> {
		private List<SampleTriangleState> states;
		private List<Long> vertices;
		private int edgeCount = 0;

		public TriangleSampler(int size) {
			this.states = new ArrayList<>();
			this.vertices = new ArrayList<>();
			this.edgeCount = 0;

			for (int i = 0; i < size; ++i) {
				states.add(new SampleTriangleState());
			}
		}

		@Override
		public void flatMap(Edge<Long, NullValue> edge, Collector<TriangleEstimate> out) throws Exception {
			// Update edge count
			edgeCount++;

			// Update vertices
			if (!vertices.contains(edge.getSource())) {
				vertices.add(edge.getSource());
			}
			if (!vertices.contains(edge.getTarget())) {
				vertices.add(edge.getTarget());
			}

			int localBetaSum = 0;

			// Process the edge for all instances
			for (SampleTriangleState state : states) {

				// Flip a coin and with probability 1/i sample a candidate
				if (vertices.size() > 2) {
					if (Coin.flip(state)) {
						state.srcVertex = edge.getSource();
						state.trgVertex = edge.getTarget();

						// Randomly sample the third vertex from V \ {src, trg}
						while (true) {
							state.thirdVertex = vertices.get((int) Math.floor(Math.random() * vertices.size()));

							if (state.thirdVertex != state.srcVertex && state.thirdVertex != state.trgVertex) {
								break;
							}
						}

						state.srcEdgeFound = false;
						state.trgEdgeFound = false;
						state.beta = 0;
					}
				}

				if (state.beta == 0) {
					// Check if any of the two remaining edges in the candidate has been found
					if ((edge.getSource() == state.srcVertex && edge.getTarget() == state.thirdVertex)
							|| (edge.getSource() == state.thirdVertex && edge.getTarget() == state.srcVertex)) {
						state.srcEdgeFound = true;
					}

					if ((edge.getSource() == state.trgVertex && edge.getTarget() == state.thirdVertex)
							|| (edge.getSource() == state.thirdVertex && edge.getTarget() == state.trgVertex)) {
						state.trgEdgeFound = true;
					}

					state.beta = (state.srcEdgeFound && state.trgEdgeFound) ? 1 : 0;
				}

				if (state.beta == 1) {
					localBetaSum++;
				}
			}

			if (localBetaSum != 0) {
				int source = getRuntimeContext().getIndexOfThisSubtask();
				out.collect(new TriangleEstimate(source, edgeCount, vertices.size(), localBetaSum));
			}
		}
	}

	private static final class TriangleSummer
			implements FlatMapFunction<TriangleEstimate, Tuple2<Integer, Integer>> {
		private Map<Integer, TriangleEstimate> results;
		private int maxEdges;
		private int maxVertices;
		private int sampleSize;
		private int previousResult;

		public TriangleSummer(int sampleSize) {
			this.results = new HashMap<>();
			this.maxEdges = 0;
			this.maxVertices = 0;
			this.sampleSize = sampleSize;
			this.previousResult = 0;
		}

		@Override
		public void flatMap(TriangleEstimate estimate, Collector<Tuple2<Integer, Integer>> out) throws Exception {
			results.put(estimate.getSource(), estimate);

			if (estimate.getEdgeCount() > maxEdges) {
				maxEdges = estimate.getEdgeCount();
			}

			if (estimate.getVertexCount() > maxVertices) {
				maxVertices = estimate.getVertexCount();
			}

			int globalBetaSum = 0;
			for (TriangleEstimate entry : results.values()) {
				globalBetaSum += entry.getBeta();
			}

			int result = (int) ((1.0 / (double) sampleSize) * globalBetaSum * maxEdges * (maxVertices - 2));
			if (result != previousResult) {
				previousResult = result;
				out.collect(new Tuple2<>(maxEdges, result));
			}

		}
	}

	private static final class SampleTriangleState implements Serializable {
		public long beta;

		public long srcVertex;
		public long trgVertex;
		public long thirdVertex;

		public boolean srcEdgeFound;
		public boolean trgEdgeFound;

		public int i;

		public SampleTriangleState() {
			this.beta = 0L;

			this.thirdVertex = -1L;
			this.srcEdgeFound = false;
			this.trgEdgeFound = false;

			i = 1;
		}
	}

	private static final class Coin {
		public static boolean flip(SampleTriangleState state) {
			boolean result = (Math.random() * (state.i) < 1);
			state.i++;

			return result;
		}
	}

	public static void main(String[] args) throws Exception {
		new BroadcastTriangleCount();
	}
}