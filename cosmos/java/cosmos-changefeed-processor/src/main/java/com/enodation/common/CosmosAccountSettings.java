

package com.enodation.common;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.lang3.StringUtils;

public class CosmosAccountSettings {

    private static final Dotenv dotenv = Dotenv.configure()
                                               .directory("src/main/resources") // Specify directory for .env
                                               .load();
    public static final String ACCOUNT_KEY =
            System.getProperty("COSMOS_ACCOUNT_KEY",
                    StringUtils.defaultString(StringUtils.trimToNull(
                                    dotenv.get("COSMOS_ACCOUNT_KEY")),
                            "FAKE_KEY_1234"));

    public static final String ACCOUNT_HOST =
            System.getProperty("COSMOS_ACCOUNT_HOST",
                    StringUtils.defaultString(StringUtils.trimToNull(
                                    dotenv.get("COSMOS_ACCOUNT_HOST")),
                            "https://localhost:8081"));


    public static final String DATABASE_NAME =
            System.getProperty("COSMOS_DATABASE_NAME",
                    StringUtils.defaultString(StringUtils.trimToNull(
                                    dotenv.get("COSMOS_DATABASE_NAME")),
                            "data-db"));

    public static final String COLLECTION_NAME =
            System.getProperty("COSMOS_COLLECTION_NAME",
                    StringUtils.defaultString(StringUtils.trimToNull(
                                    dotenv.get("COSMOS_COLLECTION_NAME")),
                            "data-coll"));

}