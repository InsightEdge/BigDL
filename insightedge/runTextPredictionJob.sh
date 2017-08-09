#!/usr/bin/env bash

#cd /code/bigdl-fork/insightedge
#mvn clean package

MASTER="spark://127.0.0.1:7077"
BASE_DIR="/code/bigdl-fork/data/textclassification" # where is the data
MODEL_FILE="/code/bigdl-fork/data/trained-model/classifier.bigdl" # where trained model is saved
$INSIGHTEDGE_HOME/bin/insightedge-submit --master ${MASTER} --driver-memory 4g --executor-memory 4g  \
           --total-executor-cores 2 --executor-cores 2 \
           --class io.insightedge.bigdl.InsightedgeTextClassifierPredictionJob \
           ./target/insightedge-0.2.0-jar-with-dependencies.jar \
           --batchSize 128 --baseDir ${BASE_DIR} --partitionNum 4 --modelFile ${MODEL_FILE}