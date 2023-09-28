package com.inalogy.midpoint.connectors.driver;

import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.identityconnectors.common.security.GuardedString;

import java.util.Arrays;
import java.util.Collections;

public class MongoClientManager {

    private final MongoDbConfiguration configuration;
    public MongoClientManager(MongoDbConfiguration mongoDbConfiguration){
        this.configuration = mongoDbConfiguration;
    }

    public MongoClient buildMongoClient() {
        char[] password = passwordAccessor(configuration.getPassword());

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder ->
                        builder.hosts(Collections.singletonList(new ServerAddress(configuration.getHost(), configuration.getPort()))))
                .credential(MongoCredential.createCredential(configuration.getUsername(), configuration.getDatabase(), password))
                .build();
        return MongoClients.create(settings);
    }



    private  char[] passwordAccessor(GuardedString guardedPassword) {
        final char[][] passwordArray = new char[1][];
        if (guardedPassword != null) {
            guardedPassword.access(chars -> passwordArray[0] = Arrays.copyOf(chars, chars.length));
        }
        return passwordArray[0];
    }


}