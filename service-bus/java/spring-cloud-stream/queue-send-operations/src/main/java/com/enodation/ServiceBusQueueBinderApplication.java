package com.enodation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.function.Supplier;

@SpringBootApplication
public class ServiceBusQueueBinderApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBusQueueBinderApplication.class);
    private static final Sinks.Many<Message<String>> many = Sinks.many().unicast().onBackpressureBuffer();

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusQueueBinderApplication.class, args);
    }

    @Bean
    public Supplier<Flux<Message<String>>> supply() {
        return ()->many.asFlux()
                .doOnNext(m->LOGGER.info("Manually sending message {}", m))
                .doOnError(t->LOGGER.error("Error encountered", t));
    }

    @Override
    public void run(String... args) {
        LOGGER.info("Going to add message {} to Sinks.Many.", "Hello World");
        for(int i=0; i<100; i++) {
            many.emitNext(MessageBuilder.withPayload("Hello World - " + Integer.toString(i)).build(), Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }

}