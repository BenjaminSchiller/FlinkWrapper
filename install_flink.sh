sudo wget -nc -O /opt/flink-1.0.3-bin-hadoop27-scala_2.11.tgz http://apache.trisect.eu/flink/flink-1.0.3/flink-1.0.3-bin-hadoop27-scala_2.11.tgz &&
sudo tar -xzvf /opt/flink-1.0.3-bin-hadoop27-scala_2.11.tgz -C /opt/ &&
grep -q 'export PATH="/opt/flink-1.0.3/bin/:$PATH"' /home/vagrant/.bashrc || echo 'export PATH="/opt/flink-1.0.3/bin/:$PATH"' >> /home/vagrant/.bashrc
