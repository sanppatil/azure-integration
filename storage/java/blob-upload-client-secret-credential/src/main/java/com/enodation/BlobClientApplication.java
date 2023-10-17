package com.enodation;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class BlobClientApplication implements CommandLineRunner {

    public static final Logger logger = LoggerFactory.getLogger(BlobClientApplication.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(BlobClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder().tenantId(env.getProperty("TENANT_ID"))
                                                                                           .clientId(env.getProperty("APP_CLIENT_ID"))
                                                                                           .clientSecret(env.getProperty("APP_CLIENT_SECRET"))
                                                                                           .build();

        BlobServiceClient storageClient = new BlobServiceClientBuilder().endpoint(env.getProperty("STORAGE_ACCOUNT_NAME"))
                                                                        .credential(clientSecretCredential)
                                                                        .buildClient();

        BlobContainerClient blobContainerClient = storageClient.getBlobContainerClient("test-cont");

        BlockBlobClient blobClient = blobContainerClient.getBlobClient("HelloWorld.txt")
                                                        .getBlockBlobClient();

        String data = "Hello world!";
        InputStream dataStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        blobClient.upload(dataStream, data.length(), true);
        logger.info("Blob uploaded successfully.");
        dataStream.close();
    }

}
