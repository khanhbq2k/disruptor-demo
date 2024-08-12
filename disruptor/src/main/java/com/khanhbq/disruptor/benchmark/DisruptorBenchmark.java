package com.khanhbq.disruptor.benchmark;

import com.khanhbq.disruptor.event.KafkaEvent;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class DisruptorBenchmark {

    private Disruptor<KafkaEvent> disruptor;
    private RingBuffer<KafkaEvent> ringBuffer;

    //    @Param({"1024", "2048", "4096"})
    @Param({"1024"})
    private int bufferSize;

    @Param({"1", "10", "100"})
    private int workHandlerSize;

    @Param({"BlockingWaitStrategy", "SleepingWaitStrategy", "YieldingWaitStrategy"})
    private String waitStrategyName;

    private WaitStrategy getWaitStrategy() {
        return switch (waitStrategyName) {
            case "BlockingWaitStrategy" -> new BlockingWaitStrategy();
            case "SleepingWaitStrategy" -> new SleepingWaitStrategy();
            case "YieldingWaitStrategy" -> new YieldingWaitStrategy();
            default -> throw new IllegalArgumentException("Unknown wait strategy");
        };
    }

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setUp() {
        disruptor = new Disruptor<>(
                KafkaEvent::new,
                bufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE,
                getWaitStrategy()
        );

        WorkHandler<KafkaEvent>[] workHandlers = new WorkHandler[workHandlerSize];
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = event -> {
                // processing something
            };
        }

        disruptor.handleEventsWithWorkerPool(workHandlers);
        ringBuffer = disruptor.start();
    }

    @Benchmark
    public void sendMessages() {
        long sequence = ringBuffer.next();
        try {
            KafkaEvent event = ringBuffer.get(sequence);
            event.setMessage("Message");
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        disruptor.shutdown();
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(DisruptorBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
