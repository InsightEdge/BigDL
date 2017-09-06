#!/usr/bin/env bash

mvn clean package
mvn install:install-file -Dfile=./common/target/common-0.2.0.jar -DgroupId=io.insightedge.bigdl \
-DartifactId=common -Dversion=0.0.1 -Dpackaging=jar