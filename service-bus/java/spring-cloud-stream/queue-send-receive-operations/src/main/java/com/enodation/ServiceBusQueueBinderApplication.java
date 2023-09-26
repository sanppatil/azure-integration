package com.enodation;

import com.azure.spring.messaging.checkpoint.Checkpointer;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

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

    @Bean
    public Consumer<Message<String>> consume() {
        return message->{
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);
            LOGGER.info("New message received: '{}'", message.getPayload());
            checkpointer.success()
                    .doOnSuccess(s->LOGGER.info("Message '{}' successfully checkpointed", message.getPayload()))
                    .doOnError(e->LOGGER.error("Error found", e))
                    .block();
        };
    }

    @Override
    public void run(String... args) {
        LOGGER.info("Going to add message {} to Sinks.Many.", "Hello World");
        many.emitNext(MessageBuilder.withPayload("Hello World").build(), Sinks.EmitFailureHandler.FAIL_FAST);
    }

}