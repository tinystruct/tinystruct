package org.tinystruct.data.tools;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.tinystruct.AbstractApplication;
import org.tinystruct.system.annotation.Action;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KafkaClient extends AbstractApplication {

    private final KafkaProducer<String, String> producer;
    private final static Logger logger = Logger.getLogger(KafkaClient.class.getName());

    public KafkaClient() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "192.168.1.101:9092");
        properties.put("ack", "all");
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());

        producer = new KafkaProducer<String, String>(properties);
    }

    @Action("send")
    public void send(ProducerRecord<String, String> record) {
        Future<RecordMetadata> future = producer.send(record);
        try {
            future.get();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            producer.close();
        }
    }

    @Override
    public void init() {
    }

    @Override
    public String version() {
        return null;
    }

}
