# Coupon Management API

A Spring Boot REST API for managing coupons. This project focuses on secure authentication, transaction management, and preventing race conditions when multiple users claim the same coupon at the same time.

## Features

* JWT authentication
* Create and update coupons
* Claim coupons securely
* Prevent duplicate claims
* Transaction management
* PostgreSQL database
* REST API

## Technologies

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Maven
* JWT

## Setup

1. Create a PostgreSQL database named `coupon_db`.
2. Update the database username and password in `application.yml`.
3. Run the project using:

```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080/api`.

## Authentication

All endpoints except `/auth/**` require a JWT token.

```
Authorization: Bearer <token>
```

## Main Endpoints

* POST `/auth/register` - Register a user
* POST `/auth/login` - Login
* POST `/coupons/create` - Create a coupon
* GET `/coupons/available` - Get available coupons
* POST `/coupons/claim` - Claim a coupon
* GET `/coupons/my-coupons` - Get claimed coupons
* GET `/coupons/my-claims` - Get claim history
* PUT `/coupons/{id}` - Update a coupon

## Concurrency Handling

The coupon claim process uses pessimistic locking and transactions to ensure only one user can update a coupon at a time. This prevents duplicate claims and keeps the data consistent.

## Project Structure

* controller
* service
* repository
* entity
* dto
* security
* config
