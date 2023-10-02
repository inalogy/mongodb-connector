## Table of Contents
1. [Introduction](#introduction)
2. [Capabilities and Features](#capabilities-and-features)
3. [Dynamic Schema](#dynamic-schema)
4. [Mongo Database Configuration](#Mongo Database Configuration)
5. [Configuration](#configuration)
6. [Customisation](#customisation)
7. [JavaDoc](#javadoc)
8. [Build](#build)
9. [TODO](#todo)
10. [Special Thanks](#special-thanks)
# Introduction
### Mongodb-connector
Mongodb(standalone mongodb) connector for midPoint 

version 0.1.0.1
# Capabilities and Features

- Schema: YES - dynamic
- Provisioning: YES
- Live Synchronization: No
- Password: No
- Activation: Yes
- Script execution: No

### Mongo Database Configuration
- keyColumn defined in connectorConfiguration should be indexed to improve performance
- keyColumn also represents icfs:name
- currently supported multivalued attributes are with a depth of 1, or one-dimensional arrays. This means the connector can handle attributes formatted as `["val1", "val2", ...]` but not nested arrays like `[["val1a", "val1b"], ["val2a", "val2b"], ...]`. All nested attributes must be strings

### database setup
1. create Database -> create Collection
2. In collection create templateUser based on which will connector create schema. Make sure your template user have all fields populated with appropriate data.
3. in connectorConfiguration define keyColumn which will represent shadow's icfs:name attr, for example: email 
4. in database createIndex for attribute specified in 3rd step
```
db.users_idm.createIndex( { "email": 1 }, {unique: true})
```

### Configuration
- Set the usual username, password, and host address

## JavaDoc
- JavaDoc can be generated locally by this command:
```bash
mvn clean javadoc:javadoc
```
## Build
```
mvn clean install
```
## Build without Tests
```
mvn clean install -DskipTests=True
```
After successful build, you can find ssh-v1.0-connector.jar in target directory.

## TODO

# Status