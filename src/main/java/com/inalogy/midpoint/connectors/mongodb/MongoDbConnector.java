package com.inalogy.midpoint.connectors.mongodb;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import com.inalogy.midpoint.connectors.driver.Connection;
import com.inalogy.midpoint.connectors.driver.MongoClientManager;
import com.inalogy.midpoint.connectors.filter.MongoDbFilter;
import com.inalogy.midpoint.connectors.schema.SchemaHandler;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;

import org.bson.conversions.Bson;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateDeltaOp;

import java.util.Set;

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
        this.connection = new Connection(mongoClientManager.buildMongoClient());
//        this.sessionManager = new SessionManager(mongoClient);
    }

    @Override
    public void dispose() {

    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> set, OperationOptions operationOptions) {
        return null;
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {

    }

    @Override
    public Schema schema() {
        return null;
    }

    @Override
    public FilterTranslator<MongoDbFilter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return null;
    }

    @Override
    public void executeQuery(ObjectClass objectClass, MongoDbFilter mongoDbFilter, ResultsHandler resultsHandler, OperationOptions operationOptions) {

    }

    @Override
    public void test() {
        try {
            MongoDatabase database = connection.getDatabase(this.configuration.getDatabase());

            Bson filter = Filters.eq(this.configuration.getKeyColumn(), this.configuration.getTemplateUser());
            FindIterable<Document> iterable = database.getCollection(this.configuration.getTable()).find(filter).limit(1);
            Document first = iterable.first();
            if (first != null) {
                System.out.println("Test successful. Found user: " + first.toJson());

                for (String key : first.keySet()) {
                    Object value = first.get(key);
                    System.out.println("Key: " + key + ", Value: " + value + ", Type: " + (value != null ? value.getClass().getSimpleName() : "null"));
                }

            } else {
                System.out.println("Test successful, but no user found with the specified email.");
            }
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
        }
    }

    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> set, OperationOptions operationOptions) {
        return null;
    }
}
