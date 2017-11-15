#!/usr/bin/env bash

#cd /code/bigdl-fork/insightedge
#mvn clean package

PAUSE_BETWEEN_BATCHES=1000 # in milliseconds

scala -cp ./spark/target/spark-0.2.0-jar-with-dependencies.jar \
        io.insightedge.bigdl.kafka.BankruptcyProducer \
        ${PAUSE_BETWEEN_BATCHES}
