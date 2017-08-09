#!/usr/bin/env bash

#cd /code/bigdl-fork/insightedge
#mvn clean package

DATA_DIR="/code/bigdl-fork/data/textclassification/20_newsgroup"

scala -cp ./target/insightedge-0.2.0-jar-with-dependencies.jar \
        io.insightedge.bigdl.kafka.Producer \
        ${DATA_DIR} 1 2