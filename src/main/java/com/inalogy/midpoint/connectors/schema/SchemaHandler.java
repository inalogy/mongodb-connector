package com.inalogy.midpoint.connectors.schema;

import com.inalogy.midpoint.connectors.utils.Constants;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SchemaHandler {
    public static final String ACCOUNT_NAME = ObjectClassUtil.createSpecialName("ACCOUNT");

    public static void buildObjectClass(SchemaBuilder schemaBuilder, String keyColumn, String passwordColumn, Document templateUser) {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        objClassBuilder.setType(ACCOUNT_NAME);
        Set<AttributeInfo> attributeInfos = buildAttributeInfoSet(templateUser, keyColumn, passwordColumn);
        objClassBuilder.addAllAttributeInfo(attributeInfos);

        schemaBuilder.defineObjectClass(objClassBuilder.build());
    }

    private static Set<AttributeInfo> buildAttributeInfoSet(Document templateUser, String keyColumn, String passwordColumn) {
        Set<AttributeInfo> attrInfo = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ISO_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(Constants.TIME_ZONE));

        for (Map.Entry<String, Object> entry : templateUser.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof org.bson.types.ObjectId) {
                value = value.toString();
            }

            if (value instanceof Date) {
                value = sdf.format((Date) value);
            }


            AttributeInfoBuilder attrBld = new AttributeInfoBuilder();

            if (name.equalsIgnoreCase(Constants.MONGODB_UID)) {
                attrBld.setName(Uid.NAME);
                attrBld.setRequired(false);  // Its generated by database
                attrBld.setUpdateable(false);
                attrInfo.add(attrBld.build());
            } else if (name.equalsIgnoreCase(keyColumn)) {
                attrBld.setName(Name.NAME);
                attrBld.setRequired(true);
                attrInfo.add(attrBld.build());
            } else if (name.equalsIgnoreCase(passwordColumn)) {
                attrInfo.add(OperationalAttributeInfos.PASSWORD);
            } else {
                Class<?> dataType = value != null ? value.getClass() : String.class; //  String.class as a default type for null values

                // Check for multi-valued attributes
                if (value instanceof List || value instanceof Set) {
                    attrBld.setMultiValued(true);
                    dataType = ((Collection<?>) value).isEmpty() ? String.class : ((Collection<?>) value).iterator().next().getClass();
                }

                attrBld.setType(dataType);
                attrBld.setName(name);
                attrBld.setRequired(false);
                attrBld.setReturnedByDefault(true); //every attr returned by default
                attrInfo.add(attrBld.build());
            }
        }
        return attrInfo;
    }

    public static ConnectorObject convertDocumentToConnectorObject(Document document, Schema schema, ObjectClass objectClass, String uniqueAttributeName) {
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
            if (value instanceof Date) {
                value = value.toString();
            }

            if (entry.getKey().equals("_id")){
                builder.setUid(new Uid(value.toString()));
                continue;
            }
            if (entry.getKey().equals(uniqueAttributeName)){
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
}
