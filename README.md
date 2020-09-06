# Security Microservice

[![Logo](https://img.shields.io/badge/vert.x-3.9.2-purple.svg)](https://vertx.io")

This application was generated using http://start.vertx.io

---

## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
* [Environment Variables](#environment-variables)
* [Setup](#setup)
* [Features](#features)
* [References](#references)
* [Contact](#contact)

---

## General info
Back-end service which provides security-related resources such as user and JSON Web Token(JWT) endpoints.

---

## Technologies
* Vert.x - Version 3.9.2
* Log4j2 - Version 2.13.3
* jBcrypt - Version 0.4

---

## Environment Variables
Following table is a **mandatory** environment variables used in this project.

| Variable Name | Datatype | Description |
| --- | --- | --- |
| **APP_PORT** | Number | Server port |
| **JWT_SECRET** | String | JWT secret key for signature of token |
| **JWT_ISSUER** | String | JWT issuer for token validation |
| **JWT_EXPIRE_IN_MINUTES** | Number | JWT token expiration period (in minutes) |
| **MONGO_HOST** | String | Mongo host |
| **MONGO_PORT** | Number | Mongo port |
| **MONGO_USERNAME** | String | Mongo username |
| **MONGO_PASSWORD** | String | Mongo password |
| **MONGO_AUTH_SOURCE** | String | Mongo database for mongo user authentication |
| **TEST_MONGO_HOST** | String | Mongo host for unit test |
| **TEST_MONGO_PORT** | Number | Mongo port for unit test |
| **TEST_MONGO_USERNAME** | String | Mongo username for unit test |
| **TEST_MONGO_PASSWORD** | String | Mongo password for unit test |
| **TEST_MONGO_AUTH_SOURCE** | String | Mongo database for mongo user authentication for unit test |

Following table is a **optional** environment variables used in this project.
| Variable Name | Datatype | Description | Default Value |
| --- | --- | --- | --- |
| **APP_HOST** | String | Server host | localhost |
| **LOG_LEVEL** | String | Log level | error |
| **GRAPHIQL_IS_ENABLE** | Boolean | Flag to enable `/graphiql` path | false

---

## Setup
To launch your tests:
```
./mvnw clean test
```

To package your application:
```
./mvnw clean package
```

To run your application:
```
./mvnw clean compile exec:java
```

---

## Features
* Can create, update, delete user resource.
* Add JWT authentication for resource handler.
* Add GraphQL to query resource.

---

## References
* [Vert.x Documentation](https://vertx.io/docs/)
* [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
* [Vert.x User Group](https://groups.google.com/forum/?fromgroups#!forum/vertx)
* [Vert.x Gitter](https://gitter.im/eclipse-vertx/vertx-users)

---

## Contact
Created by [Anas Juwaidi](mailto:anas.didi95@gmail.com)
