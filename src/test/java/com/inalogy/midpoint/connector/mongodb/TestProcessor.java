package com.inalogy.midpoint.connector.mongodb;

import org.identityconnectors.common.security.GuardedString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestProcessor {

    private MongoDbConfiguration configuration;
    protected MongoDbConnector connector;
    private final Properties properties = new Properties();


    protected TestProcessor(){
        try {
            this.loadProperties();
            this.initConfiguration();
            this.initConnector();
        } catch (IOException e) {
            System.err.println("Problem in initialization of connector");
        }
    }

    private void loadProperties() throws IOException {
        InputStream inputStream = TestProcessor.class.getClassLoader().getResourceAsStream("test.properties");

        if (inputStream == null) {
            throw new IOException("Sorry, unable to find test prop file");
        }

        properties.load(inputStream);
    }
    private void initConfiguration() {
        configuration = new MongoDbConfiguration();
        configuration.setUsername(properties.getProperty("username"));
        configuration.setPassword(new GuardedString(properties.getProperty("password").toCharArray()));
        configuration.setHost(properties.getProperty("host"));
        configuration.setPort(Integer.parseInt(properties.getProperty("port")));
        configuration.setCollection(properties.getProperty("collection"));
        configuration.setKeyColumn(properties.getProperty("keyColumn"));
        configuration.setDatabase(properties.getProperty("database"));
        configuration.setTemplateUser(properties.getProperty("templateUser"));
    }

    private void initConnector() {
        connector = new MongoDbConnector();
        connector.init(configuration);
    }
}
