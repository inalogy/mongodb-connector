package com.inalogy.midpoint.connectors.driver;

import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Manages the creation of MongoClient instances.
 * This class is responsible for configuring and building a MongoClient
 * based on the provided MongoDbConfiguration.
 * @author P-Rovnak
 * @version 1.0
 */
public class MongoClientManager {
    private static final Log LOG = Log.getLog(MongoClientManager.class);

    private final MongoDbConfiguration configuration;
    public MongoClientManager(MongoDbConfiguration mongoDbConfiguration){
        this.configuration = mongoDbConfiguration;
    }


    /**
     * Builds and returns a configured MongoClient.
     *
     * @return A MongoClient configured according to the MongoDbConfiguration.
     */
    public MongoClient buildMongoClient() {
        char[] password = passwordAccessor(configuration.getPassword());
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .credential(MongoCredential.createCredential(configuration.getUsername(), configuration.getDatabase(), password))
                .writeConcern(WriteConcern.ACKNOWLEDGED);

        // Check for Multiple hosts for replica set first
        if (configuration.getAdditionalHosts() != null && !configuration.getAdditionalHosts().isEmpty()) {
            LOG.info("building MongoClient with additional hosts");
            String[] additionalHosts = configuration.getAdditionalHosts().split(",");
            List<ServerAddress> hosts = new ArrayList<>();
            for (String host : additionalHosts) {
                hosts.add(new ServerAddress(host.trim()));
            }
            if (configuration.getHost() != null) {
                hosts.add(0, new ServerAddress(configuration.getHost(), configuration.getPort())); // Add primary host to beginning
            }
            settingsBuilder.applyToClusterSettings(builder -> builder.hosts(hosts));
        }
        // Else, use Single host & port
        else if (configuration.getHost() != null) {
            LOG.info("building MongoClient with additional hosts");
            settingsBuilder.applyToClusterSettings(builder ->
                    builder.hosts(Collections.singletonList(new ServerAddress(configuration.getHost(), configuration.getPort()))));
        }

        return MongoClients.create(settingsBuilder.build());
    }


    /**
     * Utility method to extract the password from a GuardedString.
     *
     * @param guardedPassword The GuardedString containing the password.
     * @return The password as a char array.
     */
    private  char[] passwordAccessor(GuardedString guardedPassword) {
        final char[][] passwordArray = new char[1][];
        if (guardedPassword != null) {
            guardedPassword.access(chars -> passwordArray[0] = Arrays.copyOf(chars, chars.length));
        }
        return passwordArray[0];
    }


}