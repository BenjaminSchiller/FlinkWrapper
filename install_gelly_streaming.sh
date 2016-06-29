mvn compile &&
mvn clean install &&
sudo cp ~/.m2/repository/org/apache/flink/flink-gelly-streaming/0.1.0/flink-gelly-streaming-0.1.0.jar /opt/flink-1.0.3/lib/ &&
sudo cp ~/.m2/repository/org/apache/flink/flink-gelly_2.10/1.0.3/flink-gelly_2.10-1.0.3.jar /opt/flink-1.0.3/lib/
