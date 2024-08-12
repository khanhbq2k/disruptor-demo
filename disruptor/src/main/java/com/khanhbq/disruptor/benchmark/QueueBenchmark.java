package com.khanhbq.disruptor.benchmark;

import com.khanhbq.disruptor.event.KafkaEvent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.*;

@State(Scope.Benchmark)
public class QueueBenchmark {

    private LinkedBlockingQueue<KafkaEvent> queue;
    private ExecutorService executor;
    private final Phaser phaser = new Phaser(1); // to synchronize the start of tasks

    @Param({"1024"})
    private int bufferSize;

    @Param({"1", "10", "100"})
    private int consumerCount;

    @Setup(Level.Trial)
    public void setUp() {
        queue = new LinkedBlockingQueue<>(bufferSize);
        executor = Executors.newFixedThreadPool(consumerCount + 1); // +1 for producer

        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                phaser.arriveAndAwaitAdvance(); // wait for all threads
                while (!Thread.interrupted()) {
                    try {
                        KafkaEvent event = queue.take(); // process event
                        // processing something
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    @Benchmark
    public void sendMessages() throws InterruptedException {
        KafkaEvent event = new KafkaEvent(); // Create a new event
        event.setMessage("Message");
        queue.put(event);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            System.err.println("InterruptedException during shutdown.");
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(QueueBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
