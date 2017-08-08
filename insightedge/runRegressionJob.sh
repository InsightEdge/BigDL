#!/usr/bin/env bash

cd /code/bigdl-fork/insightedge
mvn clean package

$INSIGHTEDGE_HOME/bin/insightedge-submit --master spark://127.0.0.1:7077 \
           --driver-memory 5g --executor-memory 5g  \
           --jars /code/bigdl-fork/dist/lib/bigdl-0.2.0-jar-with-dependencies.jar \
           --total-executor-cores 2 --executor-cores 2                                \
           --class io.insightedge.bigdl.LinearRegressionJob \
           /code/bigdl-fork/insightedge/target/insightedge-0.2.0-jar-with-dependencies.jar
