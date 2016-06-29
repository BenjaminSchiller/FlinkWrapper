sudo wget -nc -O /opt/flink-1.0.3-bin-hadoop27-scala_2.10.tgz http://ftp.halifax.rwth-aachen.de/apache/flink/flink-1.0.3/flink-1.0.3-bin-hadoop27-scala_2.10.tgz &&
sudo tar -xzvf /opt/flink-1.0.3-bin-hadoop27-scala_2.10.tgz -C /opt/ &&
grep -q 'export PATH="/opt/flink-1.0.3/bin/:$PATH"' /home/vagrant/.bashrc || echo 'export PATH="/opt/flink-1.0.3/bin/:$PATH"' >> /home/vagrant/.bashrc
