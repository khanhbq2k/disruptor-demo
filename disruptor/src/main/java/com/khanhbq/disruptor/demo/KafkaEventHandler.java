package com.khanhbq.disruptor.demo;

import com.khanhbq.disruptor.event.KafkaEvent;
import com.lmax.disruptor.EventHandler;

public class KafkaEventHandler implements EventHandler<KafkaEvent> {

    @Override
    public void onEvent(KafkaEvent event, long sequence, boolean endOfBatch) {
        System.out.println("Processing: " + event.getMessage());
    }
}

