#!/usr/bin/env bash

echo "-- Creating Kafka topic"
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper 127.0.0.1:2181 --replication-factor 1 --partitions 1 --topic bankruptcy
echo "-- Done."

