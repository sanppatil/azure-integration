
package com.enodation;

import com.azure.cosmos.*;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.*;
import com.enodation.common.CosmosAccountSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class CosmosChangeFeedProcessor {

    public static int WAIT_FOR_WORK = 60000;

    private static final ObjectMapper OBJECT_MAPPER = Utils.getSimpleObjectMapper();
    protected static Logger logger = LoggerFactory.getLogger(CosmosChangeFeedProcessor.class);

    private static boolean isWorkCompleted = false;

    private static ChangeFeedProcessorOptions options;



    public static void main(String[] args) {
        logger.info("Begin Sample");
        try {
            ThroughputControlGroupConfig throughputControlGroupConfig =
                    new ThroughputControlGroupConfigBuilder()
                            .groupName("cfp")
                            .targetThroughput(300)
                            .priorityLevel(PriorityLevel.LOW)
                            .build();
            options = new ChangeFeedProcessorOptions();
            options.setStartFromBeginning(false);
            options.setLeasePrefix("myChangeFeedDeploymentUnit");
            options.setFeedPollDelay(Duration.ofSeconds(5));
            options.setFeedPollThroughputControlConfig(throughputControlGroupConfig);


            CosmosAsyncClient client = getCosmosClient();
            CosmosAsyncDatabase cosmosDatabase = client.getDatabase(CosmosAccountSettings.DATABASE_NAME);
            CosmosAsyncContainer feedContainer = cosmosDatabase.getContainer(CosmosAccountSettings.COLLECTION_NAME);
            CosmosAsyncContainer leaseContainer = createNewLeaseCollection(client, CosmosAccountSettings.DATABASE_NAME, CosmosAccountSettings.COLLECTION_NAME + "-leases");

            //Model of a worker thread or application which leases access to monitor one or more feed container
            //partitions via the Change Feed. In a real-world application you might deploy this code in an Azure function.
            //The next line causes the worker to create and start an instance of the Change Feed Processor. See the implementation of getChangeFeedProcessor() for guidance
            //on creating a handler for Change Feed events. In this stream, we also trigger the insertion of 10 documents on a separate
            //thread.
            // <StartChangeFeedProcessor>
            logger.info("Start Change Feed Processor on worker (handles changes asynchronously)");
            ChangeFeedProcessor changeFeedProcessorInstance = new ChangeFeedProcessorBuilder()
                    .hostName("SampleHost_1")
                    .feedContainer(feedContainer)
                    .leaseContainer(leaseContainer)
                    .handleChanges(handleChanges())
                    .options(options)
                    .buildChangeFeedProcessor();
            changeFeedProcessorInstance.start()
                                       .subscribeOn(Schedulers.boundedElastic())
                                       .subscribe();
            // </StartChangeFeedProcessor>

            //These two lines model an application which is inserting ten documents into the feed container
            logger.info("Start application that inserts documents into feed container");

            //This loop models the Worker main loop, which spins while its Change Feed Processor instance asynchronously
            //handles incoming Change Feed events from the feed container. Of course in this sample, polling
            //isWorkCompleted is unnecessary because items are being added to the feed container on the same thread, and you
            //can see just above isWorkCompleted is set to true.
            //But conceptually the worker is part of a different thread or application than the one which is inserting
            //into the feed container; so this code illustrates the worker waiting and listening for changes to the feed container
            long remainingWork = WAIT_FOR_WORK;
            while (!isWorkCompleted && remainingWork > 0) {
                Thread.sleep(100);
                remainingWork -= 100;
            }

            //When all documents have been processed, clean up
            if (isWorkCompleted) {
                changeFeedProcessorInstance.stop().subscribe();
            } else {
                throw new RuntimeException("The change feed processor initialization and automatic create document feeding process did not complete in the expected time");
            }

            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("End Sample");
    }

    private static Consumer<List<JsonNode>> handleChanges() {
        return (List<JsonNode> docs) -> {
            logger.info("Start handleChanges()");

            for (JsonNode document : docs) {
                try {
                    //Change Feed hands the document to you in the form of a JsonNode
                    //As a developer you have two options for handling the JsonNode document provided to you by Change Feed
                    //One option is to operate on the document in the form of a JsonNode, as shown below. This is great
                    //especially if you do not have a single uniform data model for all documents.
                    logger.info("Document received: " + OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                                                                     .writeValueAsString(document));

                    //You can also transform the JsonNode to a POJO having the same structure as the JsonNode,
                    //as shown below. Then you can operate on the POJO.
                    //CustomPOJO2 pojo_doc = OBJECT_MAPPER.treeToValue(document, CustomPOJO2.class);
                    logger.info(document.asText());

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            isWorkCompleted = true;
            logger.info("End handleChanges()");

        };
    }

    public static CosmosAsyncClient getCosmosClient() {
        return new CosmosClientBuilder()
                .endpoint(CosmosAccountSettings.ACCOUNT_HOST)
                .key(CosmosAccountSettings.ACCOUNT_KEY)
                .contentResponseOnWriteEnabled(true)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildAsyncClient();
    }

    public static CosmosAsyncContainer createNewLeaseCollection(CosmosAsyncClient client, String databaseName, String leaseCollectionName) {
        CosmosAsyncDatabase databaseLink = client.getDatabase(databaseName);
        CosmosAsyncContainer leaseCollectionLink = databaseLink.getContainer(leaseCollectionName);
        CosmosContainerResponse leaseContainerResponse = null;

        try {
            leaseContainerResponse = leaseCollectionLink.read().block();

            if (leaseContainerResponse != null) {
                leaseCollectionLink.delete().block();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (RuntimeException ex) {
            if (ex instanceof CosmosException) {
                CosmosException CosmosException = (CosmosException) ex;

                if (CosmosException.getStatusCode() != 404) {
                    throw ex;
                }
            } else {
                throw ex;
            }
        }

        CosmosContainerProperties containerSettings = new CosmosContainerProperties(leaseCollectionName, "/id");
        CosmosContainerRequestOptions requestOptions = new CosmosContainerRequestOptions();

        ThroughputProperties throughputProperties = ThroughputProperties.createManualThroughput(400);

        leaseContainerResponse = databaseLink.createContainer(containerSettings, throughputProperties, requestOptions).block();

        if (leaseContainerResponse == null) {
            throw new RuntimeException(String.format("Failed to create collection %s in database %s.", leaseCollectionName, databaseName));
        }

        return databaseLink.getContainer(leaseContainerResponse.getProperties().getId());
    }

}