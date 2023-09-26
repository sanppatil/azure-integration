package com.enodation;

import com.azure.spring.messaging.checkpoint.Checkpointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import reactor.core.publisher.Sinks;
import java.util.function.Consumer;
import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

@SpringBootApplication
public class ServiceBusQueueBinderApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBusQueueBinderApplication.class);
    private static final Sinks.Many<Message<String>> many = Sinks.many().unicast().onBackpressureBuffer();

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusQueueBinderApplication.class, args);
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


}