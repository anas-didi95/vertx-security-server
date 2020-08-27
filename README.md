# Security Microservice

[![Logo](https://img.shields.io/badge/vert.x-3.9.2-purple.svg)](https://vertx.io")

This application was generated using http://start.vertx.io

---

## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
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

To-do:
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
