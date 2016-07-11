Running locally
===============
```
* `start-local.sh` to start the master
* mvn clean install
* The JAR file should be in target/flink-gelly-streaming-0.1.0-DGARunner.jar now if the build succeeds
* flink run /path/to/file.jar <edge path> <output path> <Algorithm id> <max iterations> <shortest path src vertexid Long>
```
