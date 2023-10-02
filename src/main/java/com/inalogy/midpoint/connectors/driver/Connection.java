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

    public Document alignDataTypes(Document docToInsert) {
        Document alignedDocument = new Document();

        for (Map.Entry<String, Object> entry : docToInsert.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object templateValue = templateUser.get(key);

            // if attr not present in templateUser it must be __SPECIAL_ATTR
            if (templateValue == null) {
                templateValue = templateUser.get(key);

                switch (key) {
                    case Constants.ICFS_NAME:
                        key = this.configuration.getKeyColumn();
                        break;
//                    case ICFS_UID:
//                        key = Constants.MONGODB_UID;
//                        break;
                    case ICFS_PASSWORD:
                        key = this.configuration.getPasswordColumnName();
                        break;
                    case ICFS_ACTIVATION:
                        // TODO activation
                        break;
                    default:
                    //    TODO err handling
                        break;
                }
            }

            if (templateValue != null) {
                Class<?> templateType = templateValue.getClass();
//              handle multivalue attribute (one-dimensional array) all attributes inside must be strings
                if (templateValue instanceof List) {
                    List<?> templateList = (List<?>) templateValue;
                    if (!templateList.isEmpty()) {
                        templateType = templateList.get(0).getClass();
                    }
                    List<Object> newValueList = new ArrayList<>();

                    if (value instanceof List) {
                        for (Object item : (List<?>) value) {
                            newValueList.add(convertValue(item, templateType));
                        }
                    } else {
                        newValueList.add(convertValue(value, templateType));
                    }

                    alignedDocument.append(key, newValueList);
                } else {
                    //handle any other data type
                    alignedDocument.append(key, convertValue(value, templateType));
                }
            } else {
                alignedDocument.append(key, value);
            }
        }

        return alignedDocument;
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return value.toString();
        } else if (targetType.equals(Integer.class)) {
            return Integer.parseInt(value.toString());
        } else if (targetType.equals(Long.class)) {
            return Long.parseLong(value.toString());
        } else if (targetType.equals(Double.class)) {
            return Double.parseDouble(value.toString());
        } else if (targetType.equals(Date.class)) {
//            SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
            try {
                return new Date(Date.parse(value.toString()));
            } catch (Exception e) { // Catching runtime exceptions
                //FIXME: add logging for errors
                return null;
            }

        } else if (targetType.equals(byte[].class)) {
            // Assuming the photo is Base64 encoded
            return Base64.getDecoder().decode(value.toString());
        } else if (targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(value.toString());
        } else {
            return value;
        }
    }

    public DeleteResult deleteOne(Uid uid){
        // Create a filter to match the document based on its _id
        Bson filter = Filters.eq("_id", new ObjectId(uid.getUidValue()));
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
