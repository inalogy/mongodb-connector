package com.inalogy.midpoint.connectors.driver;

import com.inalogy.midpoint.connectors.filter.MongoDbFilter;
import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;
import com.inalogy.midpoint.connectors.utils.Constants;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import org.identityconnectors.framework.common.objects.Uid;

import java.util.List;


/**
 * Manages database operations on a MongoDB instance.
 *
 * This class is responsible for executing CRUD operations on
 * MongoDB documents.
 * @author P-Rovnak
 * @version 1.0
 */
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

    /**
     * Retrieves the template user from the MongoDB collection.
     *
     * @return A Document representing the template user.
     */
    public Document getTemplateUser() {
        Bson filter = Filters.eq(this.configuration.getKeyColumn(), this.configuration.getTemplateUser());
        return collection.find(filter).first();
    }


    /**
     * Retrieves a single user Document based on a given filter.
     *
     * @param query The MongoDbFilter containing the search criteria.
     * @return A Document representing the user.
     */
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


    /**
     * Retrieves all users, optionally applying pagination.
     *
     * @param pageSize The maximum number of documents to return.
     * @param pageOffset The offset to start returning documents from.
     * @return A FindIterable<Document> containing the user documents.
     */
    public  FindIterable<Document> getAllUsers(int pageSize, int pageOffset){
        if (pageOffset == 0 && (pageSize == 0 )){
            return this.collection.find();
        } else if (pageOffset == 1 && pageSize == 1 ) {
            return null;

        } else {
            // paged find
            return this.collection.find()
                    .sort(Sorts.ascending(Constants.MONGODB_UID))
                    .skip(pageOffset)
                    .limit(pageSize);
        }
    }


    /**
     * Inserts a single Document into the MongoDB collection.
     *
     * @param document The Document to insert.
     */
    public void insertOne(Document document) {
        this.collection.insertOne(document);
    }


    /**
     * Deletes a single Document based on the provided Uid.
     *
     * @param uid The Uid of the document to delete.
     * @return The DeleteResult of the operation.
     */
    public DeleteResult deleteOne(Uid uid){
        // Create a filter to match the document based on its _id
        Bson filter = Filters.eq(Constants.MONGODB_UID, new ObjectId(uid.getUidValue()));
        return this.collection.deleteOne(filter);
    }

    /**
     * Closes the MongoClient connection.
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }


    /**
     * Updates a single user Document based on the provided Uid and update operations.
     *
     * @param uid The Uid of the document to update.
     * @param updateOps The list of update operations to apply.
     * @return The UpdateResult of the operation.
     */
    public UpdateResult updateUser(Uid uid, List<Bson> updateOps){
        // Create a filter to match the document based on its _id
        Bson filter = Filters.eq(Constants.MONGODB_UID, new ObjectId(uid.getUidValue()));
        return this.collection.updateOne(filter, Updates.combine(updateOps));


    }

}
