#!/usr/bin/env bash

#cd /code/bigdl-fork/insightedge
#mvn clean package

$INSIGHTEDGE_HOME/bin/insightedge-submit --master spark://127.0.0.1:7077 \
           --driver-memory 2g --executor-memory 2g  \
           --total-executor-cores 2 --executor-cores 2 \
           --class io.insightedge.bigdl.LinearRegressionJob \
           ./target/insightedge-0.2.0-jar-with-dependencies.jar
