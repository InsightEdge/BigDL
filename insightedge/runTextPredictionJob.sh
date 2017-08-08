#!/usr/bin/env bash

cd /code/bigdl-fork/insightedge
mvn clean package

#MASTER="127.0.0.1:7077"
BASE_DIR="/code/bigdl-fork/data/textclassification" # where is the data
$INSIGHTEDGE_HOME/bin/insightedge-submit --master spark://127.0.0.1:7077 --driver-memory 4g --executor-memory 4g  \
           --total-executor-cores 2 --executor-cores 2 \
           --class io.insightedge.bigdl.InsightedgeTextPredictionJob \
           --jars /code/bigdl-fork/dist/lib/bigdl-0.2.0-jar-with-dependencies.jar \
           /code/bigdl-fork/insightedge/target/insightedge-0.2.0-jar-with-dependencies.jar \
           --batchSize 128 --baseDir ${BASE_DIR} --partitionNum 4