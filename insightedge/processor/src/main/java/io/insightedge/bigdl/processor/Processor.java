package io.insightedge.bigdl.processor;

/**
 * @author Danylo_Hurin.
 */

import com.j_spaces.core.client.SQLQuery;

import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.openspaces.events.polling.ReceiveHandler;
import org.openspaces.events.polling.receive.ReceiveOperationHandler;
import org.openspaces.events.polling.receive.SingleTakeReceiveOperationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import io.insightedge.bigdl.model.Prediction;
import io.insightedge.bigdl.model.CallSession;


@EventDriven
@Polling(concurrentConsumers = 1, gigaSpace = "gigaMySpace")
@Component
public class Processor {

    Logger logger = Logger.getLogger(this.getClass().getName());
    @Autowired
    protected GigaSpace gigaMySpace;
    public static long counter = 1;


    public Processor() {
        logger.info("Processor started!");
    }


//    @PostConstruct
//    public void construct() {
//        logger.info("Processor construct called!");
//    }


    @EventTemplate
    SQLQuery<Prediction> template() {
        SQLQuery<Prediction> query = new SQLQuery<Prediction>(Prediction.class, "flag = 0");
        return query;
    }


    @ReceiveHandler
    ReceiveOperationHandler receiveHandler() {
        SingleTakeReceiveOperationHandler receiveHandler = new SingleTakeReceiveOperationHandler();
        receiveHandler.setNonBlocking(true);
        receiveHandler.setNonBlockingFactor(10);
        return receiveHandler;
    }


    @SpaceDataEvent
    public Prediction execute(Prediction prediction) {
        //process Data here
        String agentId = (int)(Math.random() * 10) + "";
        CallSession callSession = new CallSession(counter + "", prediction.text(), prediction.category(), agentId, prediction.timeInMilliseconds(), counter);
        prediction.setFlag(1);
        gigaMySpace.write(callSession);
        counter++;
        return prediction;

    }

}