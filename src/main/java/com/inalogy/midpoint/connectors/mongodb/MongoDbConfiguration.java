package com.inalogy.midpoint.connectors.mongodb;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.StringUtil;

import java.util.Arrays;
import java.util.List;

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
    private String templateUser;


    /**
     * User password.
     */
    private GuardedString password = null;
    private String additionalHosts;
    private boolean useTls = false;
    // Replica Set Options
    private String replicaSet;
    private String readPreference;
    private Integer maxStalenessSeconds;

    // Write Options
    private String w;
    private Boolean journal;

    // Connection Pool Options
    private Integer maxPoolSize;
    private Integer minPoolSize;
    private Integer maxIdleTimeMS;
    private Integer waitQueueMultiple;

    // Timeout Options
    private Integer serverSelectionTimeoutMS;
    private Integer connectTimeoutMS;
    private Integer socketTimeoutMS;

    // Authentication Options
    private String authSource;
    private String authMechanism;
    private String idmUpdatedAt;

    // Getters and Setters

    @ConfigurationProperty(order = 1, displayMessageKey = "Host.display", helpMessageKey = "Host.help")
    public String getHost() {
        return host;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "Database.display", helpMessageKey = "Database.help")
    public String getDatabase() {
        return database;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "Port.display", helpMessageKey = "Port.help")
    public int getPort() {
        return port;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "Username.display", helpMessageKey = "Username.help")
    public String getUsername() {
        return username;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "Collection.display", helpMessageKey = "Collection.help")
    public String getCollection() {
        return collection;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "PasswordColumnName.display", helpMessageKey = "PasswordColumnName.help")
    public String getPasswordColumnName() {
        return passwordColumnName;
    }

    @ConfigurationProperty(order = 7, displayMessageKey = "KeyColumn.display", helpMessageKey = "KeyColumn.help")
    public String getKeyColumn() {
        return keyColumn;
    }

    @ConfigurationProperty(order = 9, displayMessageKey = "TemplateUser.display", helpMessageKey = "TemplateUser.help")
    public String getTemplateUser() {
        return templateUser;
    }

    @ConfigurationProperty(order = 10, displayMessageKey = "Password.display", helpMessageKey = "Password.help")
    public GuardedString getPassword() {
        return password;
    }

    @ConfigurationProperty(order = 11, displayMessageKey = "AdditionalHosts.display", helpMessageKey = "AdditionalHosts.help")
    public String getAdditionalHosts() {
        return additionalHosts;
    }

    @ConfigurationProperty(order = 12, displayMessageKey = "UseTls.display", helpMessageKey = "UseTls.help")
    public boolean getUseTLS(){
        return useTls;
    }
    @ConfigurationProperty(order = 13, displayMessageKey = "ReplicaSet.display", helpMessageKey = "ReplicaSet.help")
    public String getReplicaSet() {
        return replicaSet;
    }

    @ConfigurationProperty(order = 14, displayMessageKey = "ReadPreference.display", helpMessageKey = "ReadPreference.help")
    public String getReadPreference() {
        return readPreference;
    }

    @ConfigurationProperty(order = 15, displayMessageKey = "MaxStalenessSeconds.display", helpMessageKey = "MaxStalenessSeconds.help")
    public Integer getMaxStalenessSeconds() {
        return maxStalenessSeconds;
    }

    @ConfigurationProperty(order = 16, displayMessageKey = "W.display", helpMessageKey = "W.help")
    public String getW() {
        return w;
    }

    @ConfigurationProperty(order = 17, displayMessageKey = "Journal.display", helpMessageKey = "Journal.help")
    public Boolean getJournal() {
        return journal;
    }

    @ConfigurationProperty(order = 18, displayMessageKey = "MaxPoolSize.display", helpMessageKey = "MaxPoolSize.help")
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    @ConfigurationProperty(order = 19, displayMessageKey = "MinPoolSize.display", helpMessageKey = "MinPoolSize.help")
    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    @ConfigurationProperty(order = 20, displayMessageKey = "MaxIdleTimeMS.display", helpMessageKey = "MaxIdleTimeMS.help")
    public Integer getMaxIdleTimeMS() {
        return maxIdleTimeMS;
    }

    @ConfigurationProperty(order = 21, displayMessageKey = "WaitQueueMultiple.display", helpMessageKey = "WaitQueueMultiple.help")
    public Integer getWaitQueueMultiple() {
        return waitQueueMultiple;
    }

    @ConfigurationProperty(order = 22, displayMessageKey = "ServerSelectionTimeoutMS.display", helpMessageKey = "ServerSelectionTimeoutMS.help")
    public Integer getServerSelectionTimeoutMS() {
        return serverSelectionTimeoutMS;
    }

    @ConfigurationProperty(order = 23, displayMessageKey = "ConnectTimeoutMS.display", helpMessageKey = "ConnectTimeoutMS.help")
    public Integer getConnectTimeoutMS() {
        return connectTimeoutMS;
    }

    @ConfigurationProperty(order = 24, displayMessageKey = "SocketTimeoutMS.display", helpMessageKey = "SocketTimeoutMS.help")
    public Integer getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    @ConfigurationProperty(order = 25, displayMessageKey = "AuthSource.display", helpMessageKey = "AuthSource.help")
    public String getAuthSource() {
        return authSource;
    }

    @ConfigurationProperty(order = 26, displayMessageKey = "AuthMechanism.display", helpMessageKey = "AuthMechanism.help")
    public String getAuthMechanism() {
        return authMechanism;
    }

    @ConfigurationProperty(order = 26, displayMessageKey = "idmUpdatedAt.display", helpMessageKey = "idmUpdatedAt.help")
    public String getIdmUpdatedAt() {
        return idmUpdatedAt;
    }

    public void setIdmUpdatedAt(String idmUpdatedAt){this.idmUpdatedAt = idmUpdatedAt;}
    public void setReplicaSet(String replicaSet) {
        this.replicaSet = replicaSet;
    }


    public void setReadPreference(String readPreference) {
        this.readPreference = readPreference;
    }

    public void setMaxStalenessSeconds(Integer maxStalenessSeconds) {
        this.maxStalenessSeconds = maxStalenessSeconds;
    }

    public void setW(String w) {
        this.w = w;
    }

    public void setJournal(Boolean journal) {
        this.journal = journal;
    }


    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public void setMaxIdleTimeMS(Integer maxIdleTimeMS) {
        this.maxIdleTimeMS = maxIdleTimeMS;
    }


    public void setWaitQueueMultiple(Integer waitQueueMultiple) {
        this.waitQueueMultiple = waitQueueMultiple;
    }

    public void setServerSelectionTimeoutMS(Integer serverSelectionTimeoutMS) {
        this.serverSelectionTimeoutMS = serverSelectionTimeoutMS;
    }

    public void setConnectTimeoutMS(Integer connectTimeoutMS) {
        this.connectTimeoutMS = connectTimeoutMS;
    }

    public void setSocketTimeoutMS(Integer socketTimeoutMS) {
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public void setAuthSource(String authSource) {
        this.authSource = authSource;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }


    @Override
    public void validate() {
        if (StringUtil.isBlank(this.getHost())) {
            throw new ConfigurationException("Host must be defined");
        }
        if (this.getPort() == -1) {
            throw new ConfigurationException("Port must be defined");
        }
        if (StringUtil.isBlank(this.getDatabase())) {
            throw new ConfigurationException("Database must be defined");
        }
        if (StringUtil.isBlank(this.getCollection())) {
            throw new ConfigurationException("Collection must be defined");
        }
        if (StringUtil.isBlank(this.getUsername())) {
            throw new ConfigurationException("Username must be defined");
        }
        if (StringUtil.isBlank(this.getKeyColumn())) {
            throw new ConfigurationException("KeyColumn (uniqueAttribute) must be defined");
        }
        if (this.getW() != null && this.getJournal() != null && this.getW().equals("0")) {
            throw new ConfigurationException("Write concern 'w=0' is incompatible with 'journal=true'");
        }
        if (this.getReplicaSet() != null && StringUtil.isBlank(this.getAdditionalHosts())) {
            throw new ConfigurationException("Additional hosts must be defined if a replica set is specified");
        }

        if (this.getW() != null) {
            List<String> validWriteConcerns = Arrays.asList("0", "1", "majority");

            if (!validWriteConcerns.contains(this.getW())) {
                throw new ConfigurationException("Invalid write concern specified. Allowed values are: 0, 1, majority");
            }

            if ("0".equals(this.getW()) && this.getJournal() != null && this.getJournal()) {
                throw new ConfigurationException("Write concern 'w=0' is incompatible with 'journal=true'");
            }
        }

        if (this.getReplicaSet() != null) {
            // TODO: better validation
            if (!this.getReplicaSet().matches("^[a-zA-Z0-9_]+$")) {
                throw new ConfigurationException("Invalid replica set name. Only alphanumeric characters are allowed.");
            }
            if (StringUtil.isBlank(this.getAdditionalHosts())) {
                throw new ConfigurationException("Additional hosts must be defined if a replica set is specified");
            }
        }
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public void setPasswordColumnName(String passwordColumnName) {
        this.passwordColumnName = passwordColumnName;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    public void setDatabase(String value) {
        this.database = value;
    }

    /**
     * Collection setter
     *
     * @param collection name value
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }


    /**
     * Key Column setter
     *
     * @param keyColumn value
     */
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }


    public void setAdditionalHosts(String additionalHosts) {
        this.additionalHosts = additionalHosts;
    }

    public void setTemplateUser(String value){this.templateUser = value;}

    public void setUseTLS(boolean useTLS){
        this.useTls = useTLS;
    }
}
