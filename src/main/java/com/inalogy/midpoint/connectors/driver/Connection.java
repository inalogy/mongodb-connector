package com.inalogy.midpoint.connectors.driver;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;


public class Connection {

    private final MongoClient mongoClient;

    public Connection(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDatabase getDatabase(String dbName) {
        if (mongoClient == null) {
            throw new IllegalStateException("Not connected to MongoDB");
        }
        return mongoClient.getDatabase(dbName);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

}
