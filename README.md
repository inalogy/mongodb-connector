## Table of Contents
1. [Introduction](#introduction)
2. [Capabilities and Features](#capabilities-and-features)
3. [Dynamic Schema](#dynamic-schema)
4. [MongoDB Database Configuration](#mongodb-database-configuration)
5. [Connector Configuration](#configuration)
6. [LiveSync](#live-sync)
7. [JavaDoc](#javadoc)
8. [Build](#build)

# Introduction
### Mongodb-connector
Mongodb connector

version 1.1.0
# Capabilities and Features

- Schema: Yes - dynamic
- Provisioning: Yes
- Live Synchronization: Yes
- Password: Yes
- Activation: Yes - simulated
- Script execution: No

### Dynamic Schema
The connector uses a **dynamic schema** based on the `templateUser` document. This template defines all attributes and their data types, serving as the foundation for the **ICF ConnId schema**.
<br>
**templateUser should be protected to avoid accidental overwrite, or made readOnly**


### Key Points
- **Required Template**: The `templateUser` must define all attributes to be processed by the connector.
- **Attribute Types**: Each attribute must have a defined type (e.g., `String`, `Boolean`, `Integer`, or `List` for multivalued attributes).
- **Multivalued Attributes**: Only one-dimensional arrays with elements of the same type are supported.

### MongoDB Database Configuration
- keyColumn represents icfs:name
- mongodb _id is represented by icfs:uid
### Supported Attributes/Data types

| **Feature**                | **Supported**                                        | **Example**                       |
|----------------------------|------------------------------------------------------|------------------------------------|
| **Array Depth**            | Depth of 1 (One-dimensional arrays)                  | `["val1", "val2", "val3"]`        |
| **Nested Arrays**          | Not supported                                        | `[["val1a", "val1b"], ["val2a", "val2b"]]` |
| **Data Types** | supported                                            | String, Boolean, Integer          |
| **Maps (Key-Value Pairs)** | Not supported                                        | `{ "key1": "value1", "key2": "value2" }`  |


### database setup
1. create Database -> create Collection -> create systemUser
2. In collection create **templateUser** based on which will connector create schema. Make sure your template user have all fields populated with appropriate data. Null values in database are treated as String data type
3. in connectorConfiguration define keyColumn which will represent shadow's icfs:name attr, for example: username 
4. in database createIndex for attribute specified in 3rd step This is crucial otherwise connector won't be able to tell if account is present in database
```
db.idmUsers.createIndex( { "username": 1 }, {unique: true})
```

### Configuration
### Required Properties
| **Property**       | **Description**                                                                                     |
|--------------------|-----------------------------------------------------------------------------------------------------|
| `Host`             | Host address of the MongoDB instance.                                                              |
| `Database`         | Name of the MongoDB database to connect to.                                                        |
| `Port`             | Port number of the MongoDB instance.                                                               |
| `Username`         | Username for authentication.                                                                       |
| `KeyColumn`        | Name of the column used as the unique identifier (key).                                    |
| `Password`         | Password for authentication.                                                                       |
| `Collection`       | Name of the MongoDB collection to use.                                                             |
| `TemplateUser`     | A JSON structure defining the schema. All attributes must be populated with appropriate data types. |

### Optional Properties
| **Property**              | **Description**                                                                                                          |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `PasswordColumnName`      | Name of the column in the database where passwords are stored.                                                           |
| `AdditionalHosts`         | Additional MongoDB hosts (comma-separated) for replica set connections.                                                  |
| `UseTLS`                  | Enables TLS for secure connections (`true` or `false`).                                                                  |
| `ReplicaSet`              | Name of the MongoDB replica set, if applicable.                                                            |
| `ReadPreference`          | Specifies the read preference for MongoDB queries (e.g., `primary`, `secondary`).                                        |
| `W`                       | Write concern level (e.g., `0`,`1`, `majority`).                                                                         |
| `Journal`                 | Enables journaling (`true` or `false`).                                                                                  |
| `idmUpdatedAt`            | Attribute name where the update timestamp will be stored (must be defined in `TemplateUser`). Injected at connector level |

## LiveSync
Configurable through connector Configuration property **liveSyncAttributeName**. In mongoDB must be defined as ISODate.

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
After successful build, you can find connector-mongodb-{**versionNumber**}.jar in target directory.

