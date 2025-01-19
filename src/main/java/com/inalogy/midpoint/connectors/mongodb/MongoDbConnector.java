package com.inalogy.midpoint.connectors.mongodb;

import com.inalogy.midpoint.connectors.mongodb.filter.MongoDbFilterTranslator;
import com.inalogy.midpoint.connectors.mongodb.utils.Constants;
import com.inalogy.midpoint.connectors.mongodb.driver.Connection;
import com.inalogy.midpoint.connectors.mongodb.driver.MongoClientManager;
import com.inalogy.midpoint.connectors.mongodb.filter.MongoDbFilter;
import com.inalogy.midpoint.connectors.mongodb.schema.SchemaHandler;

import com.mongodb.MongoException;
import com.mongodb.MongoSecurityException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.MongoWriteException;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateDeltaOp;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;


/**
 * Main class for the MongoDB Connector implementing the ConnId interfaces.
 * It provides CRUD operations for managing accounts in a MongoDB database.
 *
 * @author P-Rovnak
 * @version 1.0
 */
@ConnectorClass(displayNameKey = "mongodb.connector.display", configurationClass = MongoDbConfiguration.class)
public class MongoDbConnector implements
        PoolableConnector,
        SchemaOp,
        TestOp,
        SearchOp<MongoDbFilter>,
        CreateOp,
        UpdateDeltaOp,
        SyncOp,
        DeleteOp {
    private MongoDbConfiguration configuration;
    private Connection connection;
    private static  Schema schema = null;
    private static final Log LOG = Log.getLog(MongoDbConnector.class);
    public void checkAlive() {
        try {
            Document templateUser = this.connection.getTemplateUser();
            if (templateUser != null) {
            } else {
                LOG.error("checkAlive successful, but no user found with the specified KeyColumnName Attribute. Please check connectorConfiguration and make sure that templateUser is present in database.");
            }
        } catch (Exception e) {
            LOG.error("Connection to mongodb is not alive. " + e);
            throw new ConnectionFailedException();
        }
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public void init(Configuration configuration) {
        try {
            this.configuration = (MongoDbConfiguration) configuration;
            this.configuration.validate();
            MongoClientManager mongoClientManager = new MongoClientManager(this.configuration);
            this.connection = new Connection(mongoClientManager.buildMongoClient(), this.configuration);
        } catch (MongoSocketOpenException | MongoTimeoutException e) {
            LOG.error("FATAL_ERROR Network issue while connecting to MongoDB", e);
        } catch (MongoSecurityException e) {
            LOG.error("FATAL_ERROR Security issue in MongoDB connection", e);
        } catch (MongoException e) {
            LOG.error("FATAL_ERROR Uncategorized error while initialising mongodb connector", e);
        }
    }

    @Override
    public void dispose() {
        LOG.info("Disposing of MongoDB connector");
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                LOG.error("Error occurred while closing MongoDB connection", e);
            } finally {
                this.connection = null;
            }
        }
        schema = null;
        this.configuration = null;
    }

    /**
     * Creates a new account in MongoDB and returns its unique identifier (__UID__).
     * <p>
     * Method flow:
     * <ol>
     *   <li>Transforms the provided attributes into a MongoDB Document.</li>
     *   <li>Inserts the Document into the MongoDB collection.</li>
     *   <li>Uses the generated MongoDB _id as the ConnId __UID__ for the account.</li>
     * </ol>
     * </p>
     *
     * @return Uid The unique identifier (__UID__) for the created account, which corresponds to the MongoDB _id.
     * @throws ConnectorException if the objectClass is invalid.
     * @throws AlreadyExistsException if an account with the same unique field already exists.
     */
    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (objectClass == null || !objectClass.getObjectClassValue().equals(Constants.OBJECT_CLASS_ACCOUNT)) {
            throw new ConnectorException("Invalid object class");
        }

        Document docToInsert = new Document();
        for (Attribute attr : attributes) {
            String attrName = attr.getName();
            List<Object> attrValues = attr.getValue();

            // Handle special ICF attributes
            attrName = switch (attrName) {
                case Constants.ICFS_NAME -> this.configuration.getKeyColumn();
                case Constants.ICFS_PASSWORD -> this.configuration.getPasswordColumnName();
                default -> attrName;
            };


            // Check if it's a multi-valued attribute by its size or refer to the schema
            if (attrValues != null && attrValues.size() > 1) {
                // Multi-valued attribute, insert as an array
                docToInsert.append(attrName, attrValues);
            } else {
                // Single-valued attribute
                if (attrValues != null && !attrValues.isEmpty()) {
                    Object attrValue = attrValues.get(0);
                    if (attrValue instanceof ZonedDateTime) {
                        attrValue = Date.from(((ZonedDateTime) attrValue).toInstant());
                    }
                    docToInsert.append(attrName, attrValue);
                }
            }
        }
        try {
            this.connection.insertOne(docToInsert);
            LOG.ok("entry successfully inserted");
        } catch (MongoWriteException e) {
            if (e.getError().getCode() == Constants.MONGODB_WRITE_EXCEPTION) {
                LOG.info("alreadyExists {0}", e.getMessage());
                throw new AlreadyExistsException();
            } else {
                LOG.error("FATAL_ERROR Occurred while creating account: " + e.getMessage());
                throw new ConnectorException("FATAL_ERROR Occurred while creating account: " + e.getMessage());
            }
        }

        // Get the generated _id field from MongoDB
        Object id = docToInsert.get(Constants.MONGODB_UID);
        if (id != null) {
            return new Uid(id.toString());
        } else {
            LOG.error("FATAL_ERROR Occurred while creating account _id is Null " );
            throw new ConnectorException("Document was not inserted correctly, _id field is null");
        }
    }
    /**
     * Deletes an existing account from MongoDB.
     *
     * @throws ConnectorException if the objectClass or uid is invalid.
     * @throws UnknownUidException if the uid does not exist in the database.
     */
    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        if (objectClass == null || !objectClass.getObjectClassValue().equals(Constants.OBJECT_CLASS_ACCOUNT)) {
            throw new ConnectorException("Invalid object class");
        }

        if (uid == null || uid.getUidValue() == null) {
            throw new ConnectorException("Uid must not be null");
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
            LOG.ok("Cache schema");
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

    /**
     * Executes a query to fetch accounts from MongoDB and handles the results.
     * <p>
     * Method flow:
     * <ol>
     *   <li>Refreshes the schema if it's null.</li>
     *   <li>Initializes paging options like pageSize and pageOffset from OperationOptions.</li>
     *   <li>If a specific UID query is provided, fetches a single user. Otherwise, fetches all users based on paging options.</li>
     *   <li>Converts the MongoDB Documents to ConnectorObjects.</li>
     *   <li>Handles the results using the provided ResultsHandler.</li>
     * </ol>
     * </p>
     *
     * @throws UnknownUidException if a specific UID query returns no results.
     */
    @Override
    public void executeQuery(ObjectClass objectClass, MongoDbFilter query, ResultsHandler handler, OperationOptions options) {
        LOG.ok("executeQuery on {0}, query: {1}, options: {2}", objectClass, query, options);
        if (schema == null) {
            LOG.info("refreshing schema in executeQuery");
            schema();
        }
        int pageSize = 0;
        int pageOffset = 0;

        if (options != null) {
            Integer tempPageSize = options.getPageSize();
            Integer tempPageOffset = options.getPagedResultsOffset();

            pageSize = (tempPageSize != null) ? tempPageSize : 0;
            pageOffset = (tempPageOffset != null) ? tempPageOffset : 0;
            //empty paging should be present when running import or recon tasks
        }

        FindIterable<Document> documents;

        if (query != null) {
            // Query by specific UID/Name
            Document result = this.connection.getSingleUser(query);
            if (result != null) {
                ConnectorObject connectorObject = SchemaHandler.convertDocumentToConnectorObject(result, schema, objectClass, this.configuration);
                handler.handle(connectorObject);
                return;
            } else {
                throw new UnknownUidException();
            }
        } else {
            // Query all records
            documents = this.connection.getAllUsers(pageSize, pageOffset);
        }

        if (documents != null) {
            for (Document result : documents) {
                if (result != null) {
                    ConnectorObject connectorObject = SchemaHandler.convertDocumentToConnectorObject(result, schema, objectClass, this.configuration);
                    boolean finish = !handler.handle(connectorObject);
                    if (finish) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void test() {
        try {
            configuration.validate();
            Document templateUser = this.connection.getTemplateUser();
            if (templateUser != null) {
                LOG.info("Test successful. Found user specified in userTemplate: " + this.configuration.getTemplateUser());
            } else {
                LOG.info("Test successful, but no user found.");
            }
        } catch (Exception e) {
            LOG.error("Test failed: " + e.getMessage());
        }
    }


    /**
     * Updates an existing account in MongoDB using a set of attribute deltas.
     * <p>
     * Method flow:
     * <ol>
     *   <li>Iterates through the provided AttributeDeltas to construct MongoDB update operations.</li>
     *   <li>Executes the MongoDB update operations to modify the account.</li>
     * </ol>
     * </p>
     * Each AttributeDelta can perform one of the following:
     * <ul>
     *   <li>Add values to a multi-valued attribute.</li>
     *   <li>Remove values from a multi-valued attribute.</li>
     *   <li>Replace the value(s) of an attribute.</li>
     * </ul>
     *
     * @return Set<AttributeDelta> The set of successfully applied AttributeDeltas.
     * @throws ConnectorException if the objectClass or uid is invalid.
     * @throws UnknownUidException     if the uid does not exist in the database or no modifications were made.
     */
    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> deltas, OperationOptions operationOptions) {
        if (objectClass == null || !Constants.OBJECT_CLASS_ACCOUNT.equals(objectClass.getObjectClassValue())) {
            throw new ConnectorException("Invalid object class");
        }

        if (uid == null || uid.getUidValue() == null) {
            throw new ConnectorException("Uid must not be null");
        }

        // Initialize the update operations
        List<Bson> updateOps = new ArrayList<>();

        if (this.configuration.getIdmUpdatedAt() != null){
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String currentDateTime = sdf.format(new Date());

            String updateTimeAttribute = this.configuration.getIdmUpdatedAt();
            if (updateTimeAttribute != null) {
                Iterator<AttributeDelta> iterator = deltas.iterator();
                // we need to check if idmUpdatedAt is not already mapped in resource schema, if yes remove it
                while (iterator.hasNext()) {
                    AttributeDelta delta = iterator.next();
                    if (updateTimeAttribute.equals(delta.getName())) {
                        iterator.remove();
                        break;
                    }
                }

                updateOps.add(Updates.set(updateTimeAttribute, currentDateTime));
            }
        }
        LOG.ok("Executing updateDelta for UID: {0}", uid);
        Document templateUser = this.connection.getTemplateUser();

        for (AttributeDelta delta : deltas) {
            String attrName = delta.getName();
            attrName = switch (attrName) {
                case Constants.ICFS_NAME -> this.configuration.getKeyColumn();
                case Constants.ICFS_PASSWORD -> this.configuration.getPasswordColumnName();
                default -> attrName;
            };

            Object templateValue = templateUser.get(attrName);

            // Align values with schema
            List<Object> valuesToAdd = SchemaHandler.alignDeltaValues(delta.getValuesToAdd(), templateValue);
            List<Object> valuesToRemove = SchemaHandler.alignDeltaValues(delta.getValuesToRemove(), templateValue);
            List<Object> valuesToReplace = SchemaHandler.alignDeltaValues(delta.getValuesToReplace(), templateValue);

            // Add operations
            if (valuesToAdd != null && !valuesToAdd.isEmpty()) {
                updateOps.add(Updates.addToSet(attrName, new BasicDBObject("$each", valuesToAdd)));
            }
            if (valuesToRemove != null && !valuesToRemove.isEmpty()) {
                updateOps.add(Updates.pullAll(attrName, valuesToRemove));
            }
            if (valuesToReplace != null) {
                updateOps.add(Updates.set(attrName, valuesToReplace.isEmpty() ? null : (valuesToReplace.size() == 1 ? valuesToReplace.get(0) : valuesToReplace)));
            }
        }

        // Execute update operation
        UpdateResult result = this.connection.updateUser(uid, updateOps);
        if (result.getMatchedCount() == 0) {
            LOG.error("Unknown UID {0} in updateDelta", uid);
            throw new UnknownUidException();
        }

        LOG.ok("Successfully updated document for UID {0}. Modified count {1}", uid, result.getModifiedCount());
        return null;
    }

    @Override
    public void sync(ObjectClass objectClass, SyncToken syncToken, SyncResultsHandler syncResultsHandler, OperationOptions operationOptions) {
        // v resultHandler vratim zmenene objekty
    }

    @Override
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        //select max obligation and return it when it run last time, if not found
        return null;
    }
}
