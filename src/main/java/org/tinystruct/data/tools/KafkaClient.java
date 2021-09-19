package org.tinystruct.data.tools;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaClient extends AbstractApplication implements MessageQueue<ProducerRecord> {

    private Properties properties = new Properties();
    private KafkaProducer<String,String> producer;

    public KafkaClient() {
        properties.put("bootstrap.servers", "192.168.1.101:9092");
        properties.put("ack","all");
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());

        producer = new KafkaProducer<String, String>(properties);
    }

    @Override
    public void send(ProducerRecord record) {
//        ProducerRecord<String, String> record = new ProducerRecord<>("quickstart-events", null, "Kafka");
        Future<RecordMetadata> future = producer.send(record);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        finally {
            producer.close();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void init() {
        this.setAction("send", "send");
    }

    @Override
    public String version() {
        return null;
    }

    public static void main(String[] args) throws ApplicationException {
/*        ApplicationManager.init();
        ApplicationManager.install(new KafkaClient());
        ApplicationManager.call("send", null);*/
        System.out.println(StringSerializer.class.getName());
    }
}
