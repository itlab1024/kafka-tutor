package com.itlab1024.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerClient {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "kraft1:9092,kraft2:9092,kraft3:9092");
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 10; i++) {
                producer.send(new ProducerRecord<>("itlab1024-topic1", "itlab" + i, "itlab" + i));
            }
        }
    }
}
