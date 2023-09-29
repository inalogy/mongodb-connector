package com.inalogy.midpoint.connectors.driver;
import com.inalogy.midpoint.connectors.filter.MongoDbFilter;
import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;
import com.inalogy.midpoint.connectors.utils.Constants;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.identityconnectors.framework.spi.Configuration;

import java.util.List;



public class Connection {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final MongoDbConfiguration configuration;

    public Connection(MongoClient mongoClient, MongoDbConfiguration configuration) {
        this.mongoClient = mongoClient;
        this.configuration = configuration;
        this.database = mongoClient.getDatabase(this.configuration.getDatabase());
        this.collection = database.getCollection(this.configuration.getCollection());
    }

//    public MongoDatabase getDatabase(String dbName) {
//        if (mongoClient == null) {
//            throw new IllegalStateException("Not connected to MongoDB");
//        }
//        return mongoClient.getDatabase(dbName);
//    }


    public Document getTemplateUser() {
        Bson filter = Filters.eq(this.configuration.getKeyColumn(), this.configuration.getTemplateUser());
        return collection.find(filter).first();
    }

    public Document getSingleUser(MongoDbFilter query) {
//        Bson filter = Filters.eq(this.configuration.getKeyColumn(), name);
        if (query.byUid != null) {
            Bson filterById = Filters.eq(Constants.MONGODB_UID, new ObjectId(query.byUid));
            return collection.find(filterById).first();
        } else if (query.byName != null) {
            Bson filterByName = Filters.eq(this.configuration.getKeyColumn(), query.byName);
            return collection.find(filterByName).first();
        }
        return null;
    }

    public  FindIterable<Document> getAllUsers(){
        return this.collection.find();
    }
    public void insertOne(Document document) {
        this.collection.insertOne(document);
    }
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

}
