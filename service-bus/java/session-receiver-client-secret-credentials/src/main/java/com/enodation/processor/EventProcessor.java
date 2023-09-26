package com.enodation.processor;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.enodation.handlers.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EventProcessor implements CommandLineRunner {

	@Autowired
	private Environment env;

	@Autowired
	private EventHandler eventHandler;

	@Override
	public void run(String... args) throws Exception {

		ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
				.tenantId(env.getProperty("TENANT_ID"))
				.clientId(env.getProperty("APP_CLIENT_ID"))
				.clientSecret(env.getProperty("APP_CLIENT_SECRET"))
				.build();

		ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
				.fullyQualifiedNamespace(env.getProperty("SERVICE_BUS_NAMESPACE"))
				.credential(clientSecretCredential)
				.sessionProcessor()
				.maxConcurrentSessions(4)
				.topicName(env.getProperty("SERVICE_BUS_TOPIC_NAME"))
				.subscriptionName(env.getProperty("SERVICE_BUS_SUBSCRIPTION_NAME"))
				.disableAutoComplete()
				.receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
				.processMessage(cxt -> eventHandler.processMessage(cxt))
				.processError(cxt -> eventHandler.processError(cxt))
				.buildProcessorClient();

		processorClient.start();
	}
}
