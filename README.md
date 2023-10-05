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
- keyColumn represents icfs:name
- mongodb _id in midpoint is represented by icfs:uid (query in connector is using _id/icfs_uid aswell)
- currently supported multivalued attributes are with a depth of 1, or one-dimensional arrays. This means the connector can handle attributes formatted as `["val1", "val2", ...]` but not nested arrays like `[["val1a", "val1b"], ["val2a", "val2b"], ...]`. All nested attributes should be strings

### database setup
1. create Database -> create Collection
2. In collection create templateUser based on which will connector create schema. Make sure your template user have all fields populated with appropriate data. Null values in database are treated as String data type
3. in connectorConfiguration define keyColumn which will represent shadow's icfs:name attr, for example: email 
4. in database createIndex for attribute specified in 3rd step This is crucial otherwise connector won't be able to tell if account is present in database
```
db.idmUsers.createIndex( { "idmId": 1 }, {unique: true})
```

### Configuration
- Set the usual username, password, host address, database port, templateUser which will be used as template for generating schema, make sure all attributes of templateUser are populated with appropriate data types

## JavaDoc
- JavaDoc can be generated locally with this command:
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
After successful build, you can find mongodb-connector.jar in target directory.

## TODO

# Status