Installation
============
(OS used was Ubuntu precise x64)

Run the following scripts in order to install Apache flink 1.0.3, the gelly library version 2.10
```bash
./install_flink.sh
./install_gelly_streaming.sh
```

Please note that the scripts above is not yet idempotent. Do not run it twice. If any of the scripts failed in the middle, please clean up the files manually, before re-running.

Directory structure
===================
* `inputs`: Some sample inputs. A TSV of edges represented by Node IDs (Type: Long)
* `outputs`: Some sample outputs
* `src`: Where the main source code lives
  * In particular, the main class is `org.apache.flink.graph.streaming.example.DGARunner`
* `pom.xml`: The maven build manifest declaring dependencies and how to package the sources into output JAR files

How to run
==========
1. `start-local.sh` in order to start the flink job manager.
2. `mvn clean install` in order to install dependencies, compile the sources and package them into JARs. The JAR file should be in target/flink-gelly-streaming-0.1.0-DGARunner.jar now if the build succeeds.
3. `flink run target/flink-gelly-streaming-0.1.0-DGARunner.jar <edges_filepath> <output_filepath> <algorithm_id> <max_iterations_count> <vertexid_shortestpaths>`

Some algorithms require a maximum iterations count to break-off. Hence, `max_iterations_count` is requried for such algorithms.

Notes
=====
One some occasions, an exception might be raised indicating failure to communicate with the job manager. In this case, it could be that the job manager simply crashed due to: {faulty job, running out of memory resources}. In this case, please start the job manager again using the command in step 1 above.

Algorithm Ids
=============
* [1] Connected components
* [2] Vertext degrees
* [4] Single source shortest paths
* [5] Triangle count

