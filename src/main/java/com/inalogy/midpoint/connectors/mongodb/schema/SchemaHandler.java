package com.inalogy.midpoint.connectors.mongodb.schema;

import com.inalogy.midpoint.connectors.mongodb.utils.Constants;
import com.inalogy.midpoint.connectors.mongodb.MongoDbConfiguration;

import org.bson.Document;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for handling the schema-related functionalities in the MongoDB connector.
 * <p>
 * This class is responsible for creating ObjectClass schemas, building attributes,
 * and converting MongoDB Documents into ConnectorObjects.
 * </p>
 *
 * @author P-Rovnak
 * @version 1.0
 */
public class SchemaHandler {

    private static final Log LOG = Log.getLog(SchemaHandler.class);

    public static final String ACCOUNT_NAME = ObjectClassUtil.createSpecialName("ACCOUNT");


    /**
     * Builds the ObjectClass schema for account objects.
     *
     * @param schemaBuilder  The schema builder to which the ObjectClass definition will be added.
     * @param keyColumn      The column used as the unique identifier for the ObjectClass.
     * @param passwordColumn The column used for storing passwords.
     * @param templateUser   A MongoDB Document that serves as a template for the ObjectClass.
     */
    public static void buildObjectClass(SchemaBuilder schemaBuilder, String keyColumn, String passwordColumn, Document templateUser) {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        objClassBuilder.setType(ACCOUNT_NAME);
        Set<AttributeInfo> attributeInfos = buildAttributeInfoSet(templateUser, keyColumn, passwordColumn);
        objClassBuilder.addAllAttributeInfo(attributeInfos);

        schemaBuilder.defineObjectClass(objClassBuilder.build());
    }


    /**
     * Builds a set of AttributeInfo objects based on the attributes present in a MongoDB document.
     * <p>
     * This method iterates through the key-value pairs in the template MongoDB Document and creates
     * AttributeInfo objects based on the types and values. Special handling is done for key and password columns.
     * </p>
     *
     * @param templateUser   The MongoDB Document that serves as a template for creating AttributeInfo objects.
     * @param keyColumn      The column used as the unique identifier for the ObjectClass.
     * @param passwordColumn The column used for storing passwords.
     * @return A Set of AttributeInfo objects.
     */
    private static Set<AttributeInfo> buildAttributeInfoSet(Document templateUser, String keyColumn, String passwordColumn) {
        Set<AttributeInfo> attrInfo = new HashSet<>();

        for (Map.Entry<String, Object> entry : templateUser.entrySet()) {
            AttributeInfoBuilder attrBld = new AttributeInfoBuilder();
            String name = entry.getKey();
            Object value = entry.getValue();

            Class<?> dataType;
            boolean isMultiValued = false;

            if (value instanceof org.bson.types.ObjectId) {
                dataType = String.class;
            } else if (value instanceof Date) {
                dataType = ZonedDateTime.class;
            } else if (value instanceof org.bson.types.Binary) {
                LOG.ok("[NOT IMPLEMENTED] Skipping attribute with Binary attribute: {0}", name);
                continue;
            } else if (value instanceof org.bson.BsonTimestamp) {
                LOG.ok("[NOT IMPLEMENTED] Skipping unsupported BsonTimestamp attribute: {0}", name);
                continue;
            } else if (value instanceof Boolean) {
                dataType = Boolean.class;
            } else if (value instanceof Integer) {
                dataType = Integer.class;
            } else if (value instanceof Double || value instanceof Float) {
                LOG.ok("[NOT IMPLEMENTED] Skipping unsupported single-value type for attribute {0}: {1}", name, value.getClass().getSimpleName());
                continue;
            } else if (value instanceof List || value instanceof Set) {
                // Handle collections
                Collection<?> collection = (Collection<?>) value;
                if (collection.isEmpty()) {
                    dataType = String.class; // Default type for empty collections
                } else {
                    boolean isHomogeneous = collection.stream().map(Object::getClass).distinct().count() == 1;
                    if (!isHomogeneous) {
                        LOG.ok("Skipping heterogeneous collection attribute: {0}", name);
                        continue;
                    }

                    // Check if collection contains only supported types
                    Class<?> firstElementType = collection.iterator().next().getClass();
                    if (!(firstElementType == String.class || firstElementType == Integer.class || firstElementType == Boolean.class)) {
                        LOG.ok("Skipping unsupported collection type for attribute {0}: {1}", name, firstElementType.getSimpleName());
                        continue;
                    }
                    dataType = firstElementType;
                }
                isMultiValued = true;
            } else if (value instanceof Map) {
                // Skip embedded documents
                LOG.ok("Skipping embedded document attribute: {0}", name);
                continue;
            } else {
                // Default String for unsupported types
                dataType = String.class;
            }

            // Special handling for keyColumn and passwordColumn
            if (name.equalsIgnoreCase(Constants.MONGODB_UID)) {
                attrBld.setName(Uid.NAME);
                attrBld.setRequired(false);
                attrBld.setUpdateable(false);
                attrInfo.add(attrBld.build());
            } else if (name.equalsIgnoreCase(keyColumn)) {
                attrBld.setName(Name.NAME);
                attrBld.setRequired(true);
                attrInfo.add(attrBld.build());
            } else if (name.equalsIgnoreCase(passwordColumn)) {
                attrInfo.add(OperationalAttributeInfos.PASSWORD);
            } else {
                attrBld.setType(dataType);
                attrBld.setName(name);
                attrBld.setMultiValued(isMultiValued);
                attrBld.setRequired(false);
                attrBld.setReturnedByDefault(true); // Every attribute is returned by default
                attrInfo.add(attrBld.build());
            }
        }
        return attrInfo;
    }


    /**
     * Converts a MongoDB Document to a ConnectorObject.
     *
     * @param document      The MongoDB Document to convert.
     * @param schema        The schema used for the conversion.
     * @param objectClass   The ObjectClass type for the resulting ConnectorObject.
     * @param configuration The MongoDB Configuration.
     * @return A ConnectorObject built from the MongoDB Document.
     */
    public static ConnectorObject convertDocumentToConnectorObject(Document document, Schema schema, ObjectClass objectClass, MongoDbConfiguration configuration) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

        builder.setObjectClass(objectClass);
        ObjectClassInfo oci = schema.findObjectClassInfo(objectClass.getObjectClassValue());

        if (oci == null) {
            throw new IllegalArgumentException("ObjectClass not found in schema: " + objectClass.getObjectClassValue());
        }

        // Extract the AttributeInfo set for the ObjectClass
        Set<AttributeInfo> attributeInfos = oci.getAttributeInfo();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            //
            if (value instanceof Date) {
                value = ((Date) value).toInstant().atZone(ZoneId.of(Constants.TIME_ZONE));
            }

            if (entry.getKey().equals(Constants.MONGODB_UID)) {
                builder.setUid(new Uid(value.toString()));
                continue;
            }
            if (entry.getKey().equals(configuration.getKeyColumn())) {
                builder.setName(new Name(value.toString()));
                continue;
            }

            Optional<AttributeInfo> attributeInfoOpt = attributeInfos.stream()
                    .filter(attrInfo -> attrInfo.getName().equalsIgnoreCase(key))
                    .findFirst();

            if (attributeInfoOpt.isPresent()) {
                AttributeInfo attributeInfo = attributeInfoOpt.get();
                if (attributeInfo.isMultiValued() && value instanceof List) {
                    builder.addAttribute(AttributeBuilder.build(key, (List<?>) value));
                } else {
                    builder.addAttribute(AttributeBuilder.build(key, value));
                }
            }
        }

        return builder.build();
    }

    public static List<Object> alignDeltaValues(List<Object> deltaValues, Object templateValue) {
        if (deltaValues == null || templateValue == null) {
            return deltaValues;  // If either is null, no alignment is done.
        }

        List<Object> alignedValues = new ArrayList<>();
        Class<?> templateType = templateValue.getClass();

        // Handle multi-valued attributes
        if (templateValue instanceof List) {
            List<?> templateList = (List<?>) templateValue;
            if (!templateList.isEmpty()) {
                templateType = templateList.get(0).getClass();
            }
            for (Object value : deltaValues) {
                alignedValues.add(convertValue(value, templateType));
            }
        } else {
            // Single-valued attributes
            for (Object value : deltaValues) {
                alignedValues.add(convertValue(value, templateType));
            }
        }

        return alignedValues;
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return value.toString();

        } else if (targetType.equals(Long.class)) {
            return Long.parseLong(value.toString());

        } else if (targetType.equals(Double.class)) {
            return Double.parseDouble(value.toString());

        } else if (targetType.equals(Date.class)) {
            return  Date.from(((ZonedDateTime) value).toInstant());
        } else if (targetType.equals(byte[].class)) {
            // WORK in progress
            return Base64.getDecoder().decode(value.toString());
        } else {
            return value;
        }
    }
}
