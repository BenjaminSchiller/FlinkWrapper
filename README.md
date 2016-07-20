Installation
============
(OS used was Ubuntu precise x64)

Run the following scripts in order to install Apache flink 1.0.3, the gelly library version 2.10
```bash
./install_flink.sh
./install_gelly_streaming.sh
source ~/.bashrc
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
* On some occasions, an exception might be raised indicating failure to communicate with the job manager. In this case, it could be that the job manager simply crashed due to: {faulty job, running out of memory resources}. In this case, please start the job manager again using the command in step 1 above.

* If you get exceptions like this:
```
Caused by: java.lang.ClassNotFoundException: org.apache.flink.graph.Edge
	at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
	at java.lang.Class.forName0(Native Method)
	at java.lang.Class.forName(Class.java:278)
	at org.apache.flink.util.InstantiationUtil$ClassLoaderObjectInputStream.resolveClass(InstantiationUtil.java:64)
	at java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1620)
	at java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1521)
	at java.io.ObjectInputStream.readClass(ObjectInputStream.java:1486)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1336)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:373)
	at org.apache.flink.util.InstantiationUtil.deserializeObject(InstantiationUtil.java:290)
	at org.apache.flink.util.InstantiationUtil.readObjectFromConfig(InstantiationUtil.java:248)
	at org.apache.flink.api.java.typeutils.runtime.RuntimeSerializerFactory.readParametersFromConfig(RuntimeSerializerFactory.java:75)
	at org.apache.flink.runtime.operators.util.TaskConfig.getTypeSerializerFactory(TaskConfig.java:1088)
	... 7 more
```
This usually means that the link gelly library jar files were not correctly placed in `/opt/flink-1.0.3/lib/`. Please move them there. See `install_gelly_streaming.sh`. Also, make sure that have the correct file permissions.

A second step to that is that, you might need to kill and restart the flink job manager if it was already running in the background so as to realize the newly added files in flink's lib directory.
```
ps aux | grep flink
# Note down the flink java process id as <pid>
kill <pid>
# Start the job manager again
start-local.sh
```

To verify that flink's job manager has realized the newly added gelly libraries above, you could `ps aux | grep flink` then observe/test that the gelly libraries should be present in the -classpath parameter of the flink job manager java process.

Algorithm Ids
=============
* [1] Connected components
* [2] Vertext degrees
* [4] Single source shortest paths
* [5] Triangle count

Uninstallation
==============
Use the steps below if the installation section failed somewhere in the middle in order to clean up.

```bash
sudo rm -rf /opt/flink-1.0.3 &&
sudo rm /opt/flink-1.0.3-bin-hadoop27-scala_2.10.tgz &&
sudo rm -rf ~/.m2/repository/* # WARNING: cleans mvn repository cache. Longer download times later on during next build.
```
