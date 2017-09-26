### How to run

Used software:
* scala 2.11.8
* kafka 0.8.2.2
* insightedge 2.1
* tensorboard. To installed run: ```sudo pip intsall tensorboard```

Prerequisites:
* Download and extract data(first three steps) as described [here](https://github.com/intel-analytics/BigDL/tree/master/spark/dl/src/main/scala/com/intel/analytics/bigdl/example/textclassification)
* Set INSIGHTEDGE_HOME and KAFKA_HOME env variables
* Make sure you have Scala installed: ```scala -version```

Steps:
* Change variables according to your needs in runModelTrainingJob.sh, runTextPredictionJob.sh, runKafkaProducer.sh 
* Build the project: ```sh build.sh```
* Start zookeeper and kafka server: ```sh kafka-start.sh```
* Create Kafka topic: ```sh kafka-create-topic.sh```. To verify that topic was created run ```sh kafka-topics.sh```
* Start Insightedge in demo mode: ```sh ie-demo.sh```
* Train text classifier model: ```sh runModelTrainingJob.sh```
* Deploy BigDL/insightedge/processor/target/processor-0.2.0-jar-with-dependencies.jar in GS UI.
* In separate terminal tab or screen start Spark streaming for predictions: ```sh runTextClassificationJob.sh```.
* Open GS UI and verify that predictions are saved in io.insightedge.bigdl.model.Prediction object.
* Start Tensorboard: ```sh tensorboard.sh```. 
* In separate terminal tab or screen start web server to track CallSessions objects: ```cd web and sh runWeb.sh```. Go to https://localhost:9443 (preferable) or localhost:9000
* In separate terminal tab or screen start GS WEBUI: ```cd $INSIGHTEDGE_HOME/datagrid/bin && sh gs-webui.sh```. To connect to the grid use 'localhost' as lookup locators and 'insightedge' for lookup groups 

Shutting down:
* Stop kafka: ```sh kafka-stop.sh```
* Stop Insightedge: ```sh ie-shutdown.sh```
