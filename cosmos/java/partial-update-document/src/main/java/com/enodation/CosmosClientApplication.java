package com.enodation;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.enodation.model.Address;
import com.enodation.model.Person;
import com.nimbusds.jose.shaded.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CosmosClientApplication implements CommandLineRunner {

    public static final Logger logger = LoggerFactory.getLogger(CosmosClientApplication.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(CosmosClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        Gson gson = new Gson();

        CosmosClient cosmosClient = new CosmosClientBuilder()
                .endpoint(env.getProperty("COSMOS_HOST"))
                .key(env.getProperty("COSMOS_MASTER_KEY"))
                .gatewayMode()
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildClient();

        CosmosDatabase database = cosmosClient.getDatabase(env.getProperty("COSMOS_DATABASE_ID"));

        CosmosContainer container = database.getContainer(env.getProperty("COSMOS_CONTAINER_ID"));

        Person newPerson = Person.createPerson();
        String id = newPerson.getId();
        String partitionKey = newPerson.getPartition_key();

        container.upsertItem(newPerson, new PartitionKey(newPerson.getPartition_key()), new CosmosItemRequestOptions());
        logger.info("New document is upserted...");

        Person newPersonFromDB = container.readItem(id, new PartitionKey(partitionKey), Person.class)
                                       .getItem();
        logger.info("Existing document from database - " + gson.toJson(newPersonFromDB));

        //Replace Address for person with newer one, Note - document Id and Partition Key remains same!

        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        Address newAddress = new Address();
        newAddress.setAddressLine1("645 Middlesex Ave");
        newAddress.setAddressLine2("2nd Flr");
        newAddress.setCity("Metuchen");
        newAddress.setState("NJ");
        newAddress.setZipCode("08840");
        newAddress.setCountry("USA");

        cosmosPatchOperations.set("/MailingAddress", newAddress);

        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<Person> response = container.patchItem(newPerson.getId(), new PartitionKey(newPerson.getPartition_key()),
                cosmosPatchOperations, options, Person.class);

        logger.info("Item with ID {} has been patched", response.getItem()
                                                                .getId());

        Person patchedPersonFromDB = container.readItem(id, new PartitionKey(partitionKey), Person.class)
                                       .getItem();
        logger.info("Patched document from database - " + gson.toJson(patchedPersonFromDB));

        cosmosClient.close();
    }

}
