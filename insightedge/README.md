### How to run

Used software:
* scala 2.10.4
* kafka 0.8.2.2
* insightedge 1.0.0

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
* In separate terminal tab start kafka producer: ```sh runKafkaProducer.sh```. To verify that producer is sending messages you can run consumer which prints messages in the console: ```sh kafka-cosumer.sh```
* In separate terminal tab start Spark streaming for predictions: ```sh runTextPredictionJob.sh```.
* Open GS UI and verify that predictions are saved in io.insightedge.bigdl.model.Prediction object.

Shutting down:
* Stop kafka: ```sh kafka-stop.sh```
* Stop Insightedge: ```sh ie-shutdown.sh```
