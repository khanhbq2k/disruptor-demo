package com.khanhbq.disruptor.demo;

import com.khanhbq.disruptor.event.KafkaEvent;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executors;

public class DemoApplication {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "test-topic";

    public static void main(String[] args) {
        // Setup Producer
        KafkaProducer<String, String> producer = createProducer();

        // Produce messages
        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>(TOPIC, "key-" + i, "message-" + i));
            System.out.println("Sent: message-" + i);
        }
        producer.close();

        // Setup Consumer and Disruptor
        KafkaConsumer<String, String> consumer = createConsumer();
        setupDisruptor(consumer);
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    private static void setupDisruptor(KafkaConsumer<String, String> consumer) {
        // Disruptor Setup
        EventFactory<KafkaEvent> factory = KafkaEvent::new;
        int bufferSize = 1024;
        Disruptor<KafkaEvent> disruptor = new Disruptor<>(factory, bufferSize, Executors.defaultThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        disruptor.handleEventsWith(new KafkaEventHandler());
        disruptor.start();

        // Connecting Disruptor with Kafka
        RingBuffer<KafkaEvent> ringBuffer = disruptor.getRingBuffer();
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    long sequence = ringBuffer.next();
                    try {
                        KafkaEvent event = ringBuffer.get(sequence);
                        event.setMessage(record.value());
                    } finally {
                        ringBuffer.publish(sequence);
                    }
                }
            }
        } finally {
            disruptor.shutdown();
            consumer.close();
        }
    }
}