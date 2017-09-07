#!/usr/bin/env bash

#cd /code/bigdl-fork/insightedge
#mvn clean package

MASTER="spark://127.0.0.1:7077"
BASE_DIR="$IE_BIGDL_DIR/data/textclassification" # where is the data
MODEL_FILE="$IE_BIGDL_DIR/data/trained-model/classifier.bigdl" # where to save model
$INSIGHTEDGE_HOME/bin/insightedge-submit --master ${MASTER} --driver-memory 10g --executor-memory 10g  \
           --total-executor-cores 2 --executor-cores 2 \
           --class io.insightedge.bigdl.InsightedgeTextClassifierTrainingJob \
           ./spark/target/spark-0.2.0-jar-with-dependencies.jar \
           --batchSize 128 --baseDir ${BASE_DIR} --partitionNum 4 \
           --modelFile ${MODEL_FILE} --epochNum 20 --embeddingDim 100
