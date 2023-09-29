package com.inalogy.midpoint.connectors.filter;

import com.inalogy.midpoint.connectors.mongodb.MongoDbConnector;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;

public class MongoDbFilterTranslator extends AbstractFilterTranslator<MongoDbFilter> {
    private static final Log LOG = Log.getLog(MongoDbFilterTranslator.class);

    @Override
    protected MongoDbFilter createEqualsExpression(EqualsFilter filter, boolean not) {
        LOG.ok("createEqualsExpression, filter: {0}, not: {1}", filter, not);

        if (not) {
            System.out.println("not supported");
            LOG.ok("not in equalsExpression");
            return null;            // not supported
        }
        Attribute attr = filter.getAttribute();
        LOG.ok("attr.getName:  {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName())) {
            MongoDbFilter lookingFor = new MongoDbFilter();
            lookingFor.byUid = String.valueOf(attr.getValue().get(0));
            return lookingFor;
        }
        return null;            // not supported
    }
}

