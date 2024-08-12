package com.khanhbq.disruptor.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaEvent {

    private String message;
}