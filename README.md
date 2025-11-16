# Cleaning Booking Service

A Spring Boot backend service for managing bookings, cleaner availability, and vehicle assignments.

## Features

* Create and update bookings
* Check availability of cleaners and vehicles
* Prevent overlapping bookings
* Auto-create BOOKED and BREAK availability blocks
* Friday bookings are not allowed
* Fully validated request payloads
* Uses schema.sql and data.sql for database initialization
* Swagger documentation available

---

## Requirements

* Java 21+
* Maven 3.8+
* MySQL 8.x

---

## Database Setup

Create a MySQL database:

```sql
CREATE DATABASE cleaning_booking;
```

---

## Application Properties

Use the following configuration inside
`src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cleaning_booking?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

spring.sql.init.mode=always
spring.sql.init.platform=mysql

spring.profiles.active=dev
```

The project contains:

* `schema.sql` — creates tables
* `data.sql` — inserts seed test data

These run automatically.

---

## Running the Project

Before starting the application makesure that to use your database url, password and username
To start the application:

```bash
mvn spring-boot:run
```

Or build and run manually:

```bash
mvn clean package
java -jar target/booking-service-0.0.1-SNAPSHOT.jar
```

---

## Swagger Documentation

After the server starts:

Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```
http://localhost:8080/v3/api-docs
```

---

## Testing

Run all tests using:

```bash

mvn test
```

---

## Notes

* Working hours are 08:00 to 22:00.
* Duration hours must be **2** or **4**.
* Cleaners per booking must be between **1 and 3**.
* BREAK block of 30 minutes is added after each booking.
* The system prevents overlapping or duplicate bookings for the same cleaner.