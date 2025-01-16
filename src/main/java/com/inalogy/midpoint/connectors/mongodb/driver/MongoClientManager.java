package com.inalogy.midpoint.connectors.mongodb.driver;

import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
                .credential(MongoCredential.createCredential(configuration.getUsername(), configuration.getDatabase(), password));

        if (configuration.getUseTLS()) {
            // Initialize SSL Context and get pubk from midpoint keystore
            SSLContext sslContext;
            try {
                sslContext = createSSLContext();
                LOG.info("Applying SSLContext");
            } catch (Exception e) {
                LOG.error("Failed to initialize SSL context", e);
                throw new RuntimeException("Failed to initialize SSL context", e);
            }
            settingsBuilder.applyToSslSettings(builder -> builder.enabled(true).context(sslContext));
        }

        // Multiple hosts for replica set
        if (configuration.getAdditionalHosts() != null && !configuration.getAdditionalHosts().isEmpty()) {
            LOG.ok("building MongoClient with additional hosts");
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

        // Set replica set
        if (configuration.getReplicaSet() != null) {
            settingsBuilder.applyToClusterSettings(builder -> builder.requiredReplicaSetName(configuration.getReplicaSet()));
        }

        // Set read preference
        if (configuration.getReadPreference() != null) {
            settingsBuilder.readPreference(ReadPreference.valueOf(configuration.getReadPreference()));
        }

        // Set write concern
        if (configuration.getW() != null) {
            WriteConcern wc;
            String wValue = configuration.getW();
            try {
                int wInt = Integer.parseInt(wValue);
                wc = new WriteConcern(wInt);
            } catch (NumberFormatException e) {
                // Not an integer, treat as a string
                if (!wValue.equals("majority")){
                    LOG.error("Received invalid writeConcern in configuration Property");
                    throw new ConfigurationException("Received invalid writeConcern");
                }
                wc = new WriteConcern(wValue);
            }

            if (configuration.getJournal() != null) {
                wc = wc.withJournal(configuration.getJournal());
            }

            settingsBuilder.writeConcern(wc);
        }

        // Single host & port
        if (configuration.getHost() != null && (configuration.getAdditionalHosts() == null || configuration.getAdditionalHosts().isEmpty())) {
            LOG.ok("building MongoClient with single host");
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

    private SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        // Initialize default trust manager
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        TrustManager[] trustManagers = tmf.getTrustManagers();

        // Initialize SSLContext with default trust managers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }
}