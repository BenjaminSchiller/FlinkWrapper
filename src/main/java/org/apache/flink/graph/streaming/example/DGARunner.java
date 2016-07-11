package org.apache.flink.graph.streaming.example;

import org.apache.flink.api.common.ProgramDescription;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Triplet;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.*;
//import org.apache.flink.graph.library.similarity.*;
//import org.apache.flink.graph.library.clustering.directed.*;
import org.apache.flink.types.NullValue;

/**
 * This example shows how to use Gelly's library methods.
 * You can find all available library methods in {@link org.apache.flink.graph.library}. 
 * 
 * In particular, this example uses the {@link GSAConnectedComponents}
 * library method to compute the connected components of the input graph.
 *
 * The input file is a plain text file and must be formatted as follows:
 * Edges are represented by tuples of srcVertexId, trgVertexId which are
 * separated by tabs. Edges themselves are separated by newlines.
 * For example: <code>1\t2\n1\t3\n</code> defines two edges,
 * 1-2 with and 1-3.
 *
 */
public class DGARunner implements ProgramDescription {

	@SuppressWarnings("serial")
	public static void main(String [] args) throws Exception {

		if(!parseParameters(args)) {
			return;
		}

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		DataSet<Edge<Long, NullValue>> edges = getEdgesDataSet(env);

		Graph<Long, Long, NullValue> graph = Graph.fromDataSet(edges, new MapFunction<Long, Long>() {
			@Override
			public Long map(Long value) throws Exception {
				return value;
			}
		}, env);
		
		DataSet result;

		// Run the selected algorithm based on input parameters
		if (algorithmId.equals("1")) {
		// Connected components
		result = graph
				.run(new GSAConnectedComponents<Long, NullValue>(maxIterations));
		} else if (algorithmId.equals("2")) {
		// Vertex degrees
		result = graph.getDegrees();
		} else if (algorithmId.equals("3")) {
		// Finding Motifs
		graph = graph.subgraph(
		new FilterFunction<Vertex<Long, Long>>() {
			   	public boolean filter(Vertex<Long, Long> vertex) {
					return (vertex.getId() > 2);
			   }
		   },
		new FilterFunction<Edge<Long, NullValue>>() {
				public boolean filter(Edge<Long, NullValue> edge) {
					return (edge.getTarget() == 4);
				}
		});
		result = graph.getTriplets();
		} else if (algorithmId.equals("4")) {
		// Shortest paths

		Graph<Long, Double, NullValue> graph1 = graph.mapVertices(new MapFunction<Vertex<Long, Long>,Double>() {
			@Override
			public Double map(Vertex<Long, Long> vertexValue) {
				return (double) vertexValue.getValue();
			}
		});

		Graph<Long, Double, Double> graph2 = graph1.mapEdges(new MapFunction<Edge<Long, NullValue>, Double>() {
			@Override
			public Double map(Edge<Long, NullValue> edgeValue) {
				return 1.0;
			}
		});
		
		
		result = (new SingleSourceShortestPaths<Long>(shortestPathsVertextId, maxIterations)).run(graph2);
		} else if (algorithmId.equals("5")) {
		// Triangle count
		result = graph
				.run(new GSATriangleCount<Long, Long, NullValue>());
		} else if (algorithmId.equals("6")) {
		// Triangle listing
		result = graph
				.run(new TriangleEnumerator<Long, Long, NullValue>());
		} /*else if (algorithmId == "7") {
		// Jaccard Index
		DataSet result = graph
				.run(new JaccardIndex<Long, NullValue>());
		} else if (algorithmId == "8") {
		// Local clustering coeff.
		DataSet result = graph
				.run(new LocalClusteringCoefficient<Long, NullValue>());
		} else if (algorithmId == "9") {
		// Global clustering coeff.
		DataSet<Vertex<Long, Long>> result = graph
				.run(new GlobalClusteringCoefficient<Long, NullValue>());
		} */else {
			result = null;
		}


		// emit result
		if (fileOutput) {
			try {
				result.writeAsCsv(outputPath, "\n", "\t");
			} catch (IllegalArgumentException e) {
				// writeAsCSV() can only be called on datasets of tuples
				// Triangle counting only returns a dataset of ONE integer
				result.writeAsText(outputPath);
			}

			// since file sinks are lazy, we trigger the execution explicitly
			env.execute("DGARunner");
		} else {
			result.print();
		}
	}

	@Override
	public String getDescription() {
		return "DGARunner";
	}

	// *************************************************************************
	// UTIL METHODS
	// *************************************************************************

	private static boolean fileOutput = false;
	private static String edgeInputPath = null;
	private static String outputPath = null;
	private static String algorithmId = null;
  	
	// Here we hardcode the maxIterations for the connected components algorithm
	private static Integer maxIterations = 4;
	private static long shortestPathsVertextId = 1L;

	// Parsing input params
	private static boolean parseParameters(String [] args) {
		if(args.length > 0) {
			if(args.length != 5) {
				System.err.println("Usage DGARunner <edge path> <output path> " +
						"<Algorithm id> <max iterations> <shortest path src vertexid Long>");
				return false;
			}

			fileOutput = true;
			edgeInputPath = args[0];
			outputPath = args[1];
			algorithmId = args[2];
			maxIterations = Integer.parseInt(args[3]);
			shortestPathsVertextId = Long.parseLong(args[4]);

		} else {
			System.out.println("Executing DGARunner example with default parameters and built-in default data.");
			System.out.println("Provide parameters to read input data from files.");
			System.out.println("Usage DGARunner <edge path> <output path> " +
					"<Algorithm id> <max iterations> <shortest path src vertexid Long>`");
		}

		return true;
	}

	@SuppressWarnings("serial")
	private static DataSet<Edge<Long, NullValue>> getEdgesDataSet(ExecutionEnvironment env) {

		if(fileOutput) {
			return env.readCsvFile(edgeInputPath)
					.ignoreComments("#")
					.fieldDelimiter("\t")
					.lineDelimiter("\n")
					.types(Long.class, Long.class)
					.map(new MapFunction<Tuple2<Long, Long>, Edge<Long, NullValue>>() {
						@Override
						public Edge<Long, NullValue> map(Tuple2<Long, Long> value) throws Exception {
							return new Edge<>(value.f0, value.f1, NullValue.getInstance());
						}
					});
		} else {
			return null;
		}
	}
}
