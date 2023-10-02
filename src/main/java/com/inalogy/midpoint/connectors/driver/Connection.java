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
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import static com.inalogy.midpoint.connectors.utils.Constants.ICFS_ACTIVATION;
import static com.inalogy.midpoint.connectors.utils.Constants.ICFS_PASSWORD;
import static com.inalogy.midpoint.connectors.utils.Constants.ICFS_UID;


public class Connection {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final MongoDbConfiguration configuration;
    private final Document templateUser;

    public Connection(MongoClient mongoClient, MongoDbConfiguration configuration) {
        this.mongoClient = mongoClient;
        this.configuration = configuration;
        this.database = mongoClient.getDatabase(this.configuration.getDatabase());
        this.collection = database.getCollection(this.configuration.getCollection());
        this.templateUser = this.getTemplateUser();
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

    public DeleteResult deleteOne(Uid uid){
        // Create a filter to match the document based on its _id
        Bson filter = Filters.eq(Constants.MONGODB_UID, new ObjectId(uid.getUidValue()));
        return this.collection.deleteOne(filter);
    }
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public UpdateResult updateUser(Uid uid, List<Bson> updateOps){
        // Create a filter to match the document based on its _id
        Bson filter = Filters.eq(Constants.MONGODB_UID, new ObjectId(uid.getUidValue()));
        return this.collection.updateOne(filter, Updates.combine(updateOps));


    }

}
