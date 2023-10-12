package com.inalogy.midpoint.connectors.mongodb;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class MongoDbConfiguration extends AbstractConfiguration {

    /**
     * Server hostname.
     */
    private String host;
    /**
     * Database name.
     */
    private String database;
    /**
     * Server port.
     */
    private int port = -1;

    /**
     * Username of the user, used for authentication.
     */
    private String username;

    private String collection;


    private String passwordColumnName;

    private String keyColumn;
    private String jdbcUrlTemplate;
    private String templateUser;


    /**
     * User password.
     */
    private GuardedString password = null;
    private String additionalHosts;

    @Override
    public void validate() {
        //FIXME: not working...
        if (this.getHost() == null || this.getHost().isEmpty()){
            throw new ConfigurationException("Host must be defined");
        }
        if (this.getPort() ==  -1){
            throw new ConfigurationException("Port must be defined");
        }
        if (this.getDatabase() == null || this.getDatabase().isEmpty()){
            throw new ConfigurationException("Database must be defined");
        }
        if (this.getCollection() == null || this.getCollection().isEmpty()){
            throw new ConfigurationException("Collection must be defined");
        }
        if (this.getUsername() == null || this.getUsername().isEmpty()){
            throw new ConfigurationException("Username must be defined");
        }
        if (this.getKeyColumn() == null || this.getKeyColumn().isEmpty()){
            throw new ConfigurationException("KeyColumn (uniqueAttribute) must be defined");
        }
    }
    @ConfigurationProperty(order = 100)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @ConfigurationProperty(order = 101)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @ConfigurationProperty(order = 102)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordColumnName() {
        return passwordColumnName;
    }

    public void setPasswordColumnName(String passwordColumnName) {
        this.passwordColumnName = passwordColumnName;
    }
    @ConfigurationProperty(order = 103)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 6,
            displayMessageKey = "DATABASE_DISPLAY",
            helpMessageKey = "DATABASE_HELP")
    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String value) {
        this.database = value;
    }

    /**
     * Database Table name. The name of the identity holder table (Integration table).
     */

    /**
     * The table name
     *
     * @return the user account table name
     * Please notice, there are used non default message keys
     */
    @ConfigurationProperty(order = 7,
            displayMessageKey = "COLLECTION_DISPLAY",
            helpMessageKey = "COLLECTION_HELP")
    public String getCollection() {
        return this.collection;
    }

    /**
     * Table setter
     *
     * @param collection name value
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * Key Column getter
     *
     * @return keyColumn value
     */
    @ConfigurationProperty(order = 8,
            displayMessageKey = "KEY_COLUMN_DISPLAY",
            helpMessageKey = "KEY_COLUMN_HELP")
    public String getKeyColumn() {
        return this.keyColumn;
    }

    /**
     * Key Column setter
     *
     * @param keyColumn value
     */
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    @ConfigurationProperty(order = 106)
    public String getAdditionalHosts() {
        return additionalHosts;
    }

    public void setAdditionalHosts(String additionalHosts) {
        this.additionalHosts = additionalHosts;
    }

    public String getTemplateUser() {return templateUser;}

    public void setTemplateUser(String value){this.templateUser = value;}


}
