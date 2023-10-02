package com.inalogy.midpoint.connectors.mongodb;
import com.inalogy.midpoint.connectors.filter.MongoDbFilterTranslator;
import com.inalogy.midpoint.connectors.utils.Constants;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import com.inalogy.midpoint.connectors.driver.Connection;
import com.inalogy.midpoint.connectors.driver.MongoClientManager;
import com.inalogy.midpoint.connectors.filter.MongoDbFilter;
import com.inalogy.midpoint.connectors.schema.SchemaHandler;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import static com.mongodb.client.model.Filters.eq;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateDeltaOp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ConnectorClass(displayNameKey = "mongodb.connector.display", configurationClass = MongoDbConfiguration.class)

public class MongoDbConnector implements
        PoolableConnector,
        SchemaOp,
        TestOp,
        SearchOp<MongoDbFilter>,
        CreateOp,
        UpdateDeltaOp,
        DeleteOp {
//    private SessionManager MongoClientManager;
    private MongoDbConfiguration configuration;
    private static SchemaHandler schemaHandler = null;
    private MongoClientManager mongoClientManager;
    private Connection connection;
//    private static SchemaCache schemaCache;

    private static  Schema schema = null;
    private static final Log LOG = Log.getLog(MongoDbConnector.class);
    @Override
    public void checkAlive() {

    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = (MongoDbConfiguration) configuration;
        this.configuration.validate();
        this.mongoClientManager = new MongoClientManager(this.configuration);
        this.connection = new Connection(mongoClientManager.buildMongoClient(), this.configuration);
//        this.sessionManager = new SessionManager(mongoClient);
    }

    @Override
    public void dispose() {
        LOG.info("Disposing MognoDb connector");
        if (this.connection != null){
            this.connection.close();
//            schemaCache = null;
            schema = null;
        }

    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (objectClass == null || !objectClass.getObjectClassValue().equals("__ACCOUNT__")) {
            throw new IllegalArgumentException("Invalid object class");
        }

        Document docToInsert = new Document();
        for (Attribute attr : attributes) {
            String attrName = attr.getName();
            List<Object> attrValues = attr.getValue();

            // Check if it's a multi-valued attribute by its size or refer to the schema
            if (attrValues != null && attrValues.size() > 1) {
                // Multi-valued attribute, insert as an array
                docToInsert.append(attrName, attrValues);
            } else {
                // Single-valued attribute
                if (attrValues != null && !attrValues.isEmpty()) {
                    Object attrValue = attrValues.get(0);
                    docToInsert.append(attrName, attrValue);
                }
            }
        }
        Document transformedDocument = this.connection.alignDataTypes(docToInsert);
        // Insert the document into MongoDB
        this.connection.insertOne(transformedDocument);

        // Get the generated _id field from MongoDB and return it as a Uid
        Object id = transformedDocument.get(Constants.MONGODB_UID);
        if (id != null) {
            return new Uid(id.toString());
        } else {
            throw new IllegalStateException("Document was not inserted correctly, _id field is null");
        }
    }
    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        if (objectClass == null || !objectClass.getObjectClassValue().equals("__ACCOUNT__")) {
            throw new IllegalArgumentException("Invalid object class");
        }

        if (uid == null || uid.getUidValue() == null) {
            throw new IllegalArgumentException("Uid must not be null");
        }

        DeleteResult result = this.connection.deleteOne(uid);
        // Check if a document was actually deleted
        if (result.getDeletedCount() == 0) {
            throw new UnknownUidException();
        }
    }

    @Override
    public Schema schema() {
        if (schema == null) {
            LOG.info("Cache schema");
//            SchemaCache schemaCache = new SchemaCache(this.connection.getTemplateUser(), configuration);
            SchemaBuilder schemaBuilder = new SchemaBuilder(MongoDbConnector.class);
            SchemaHandler.buildObjectClass(schemaBuilder, this.configuration.getKeyColumn(), this.configuration.getPasswordColumnName(), this.connection.getTemplateUser());

            // Build the schema
            schema = schemaBuilder.build();
        }
        return schema;
    }

    @Override
    public FilterTranslator<MongoDbFilter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return new MongoDbFilterTranslator();
    }

    @Override
    public void executeQuery(ObjectClass objectClass, MongoDbFilter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQuery on {0}, query: {1}, options: {2}", objectClass, query, options);
//        FIXME:PAGING!
        if (schema == null){
            LOG.info("refreshing schema in executeQuery");
            schema();
        }

        if (query != null && query != null) {
            // Query by specific UID
            Document result = this.connection.getSingleUser(query);
            if (result != null) {
                ConnectorObject connectorObject = SchemaHandler.convertDocumentToConnectorObject(result, schema, objectClass, this.configuration.getKeyColumn());
                handler.handle(connectorObject);
            }
        } else {
            // Query all records
            FindIterable<Document> allDocuments = this.connection.getAllUsers();
            for (Document result : allDocuments) {
                ConnectorObject connectorObject = SchemaHandler.convertDocumentToConnectorObject(result, schema, objectClass, this.configuration.getKeyColumn());
                handler.handle(connectorObject);
            }
        }
    }

    @Override
    public void test() {
        try {
            Document templateUser = this.connection.getTemplateUser();
            if (templateUser != null) {
                System.out.println("Test successful. Found user: " + templateUser.toJson());

                for (String key : templateUser.keySet()) {
                    Object value = templateUser.get(key);
                    LOG.ok("Key: " + key + ", Value: " + value + ", Type: " + (value != null ? value.getClass().getSimpleName() : "null"));
//                    FIXME: remove later
                }

            } else {
                LOG.ok("Test successful, but no user found with the specified email.");
            }
        } catch (Exception e) {
            LOG.error("Test failed: " + e.getMessage());
        }
    }

    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> deltas, OperationOptions operationOptions) {
        if (objectClass == null || !objectClass.getObjectClassValue().equals("__ACCOUNT__")) {
            throw new IllegalArgumentException("Invalid object class");
        }

        if (uid == null || uid.getUidValue() == null) {
            throw new IllegalArgumentException("Uid must not be null");
        }



        // Initialize an empty set to keep track of successfully applied AttributeDeltas
        Set<AttributeDelta> appliedDeltas = new HashSet<>();

        // Initialize the update operations
        List<Bson> updateOps = new ArrayList<>();

        for (AttributeDelta delta : deltas) {
            String attrName = delta.getName();
            List<Object> valuesToAdd = delta.getValuesToAdd();
            List<Object> valuesToRemove = delta.getValuesToRemove();
            List<Object> valuesToReplace = delta.getValuesToReplace();

            // Handling add operations
            if (valuesToAdd != null && !valuesToAdd.isEmpty()) {
                updateOps.add(Updates.addToSet(attrName, new BasicDBObject("$each", valuesToAdd)));
            }

            // Handling remove operations
            if (valuesToRemove != null && !valuesToRemove.isEmpty()) {
                updateOps.add(Updates.pullAll(attrName, valuesToRemove));
            }
            // Handling replace operations
            if (valuesToReplace != null && !valuesToReplace.isEmpty()) {
                updateOps.add(Updates.set(attrName, valuesToReplace.size() == 1 ? valuesToReplace.get(0) : valuesToReplace));
            }
            // Assume delta is successfully applied
            appliedDeltas.add(delta);
        }

        // Perform the update operation
        UpdateResult result = this.connection.updateUser(uid, updateOps);

        // Check if a document was actually updated
        if (result.getModifiedCount() == 0) {
            throw new IllegalArgumentException("Fatal error occurred while updating projections with _id: " + uid);
        }

        return appliedDeltas;
    }

}
