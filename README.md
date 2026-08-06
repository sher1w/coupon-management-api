# Coupon Management API

 The project focuses on secure authentication, coupon management, and maintaining data consistency during concurrent requests.

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL
- Maven

### Setup

1. Create the database:
```bash
createdb coupon_db
```

2. Copy `application-example.yml` to `application.yml` and update it with your PostgreSQL credentials and JWT secret.

3. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

The API will run on `http://localhost:8080/api`

## Testing the API

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

Save the `token` from the response.

### 2. Create a Coupon

```bash
curl -X POST http://localhost:8080/api/coupons/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SUMMER2024",
    "discountValue": 20.00,
    "discountType": "PERCENTAGE",
    "totalQuantity": 10,
    "expiryDate": "2025-12-31T23:59:59"
  }'
```

### 3. Claim a Coupon

```bash
curl -X POST http://localhost:8080/api/coupons/claim \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"couponCode": "SUMMER2024"}'
```

Response: `"success": true`

### 4. Try to Claim the Same Coupon Again

Run the same command as step 3.

Response: `"success": false, "message": "You have already claimed this coupon"`

This demonstrates the duplicate claim prevention working correctly.

### 5. View Your Claimed Coupons

```bash
curl -X GET http://localhost:8080/api/coupons/my-coupons \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## How Concurrency is Handled

The system prevents race conditions when multiple users claim the same coupon simultaneously:

1. **Pessimistic Write Locking** - The database locks the coupon row so only one user can access it at a time
2. **SERIALIZABLE Isolation** - The claim operation uses the SERIALIZABLE isolation level to provide stronger consistency during concurrent transactions
3. **Atomic Transactions** - All operations (check stock, increment counter, save claim) happen together or not at all

When multiple users attempt to claim a limited number of coupons simultaneously, only the available number of claims are processed successfully while the remaining requests receive an appropriate error response.

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Login and get JWT token |
| POST | `/coupons/create` | Create new coupon |
| GET | `/coupons/available` | List all available coupons |
| GET | `/coupons/code/{code}` | Get coupon by code |
| POST | `/coupons/claim` | Claim a coupon |
| GET | `/coupons/my-coupons` | Get your claimed coupons |
| GET | `/coupons/my-claims` | Get your claim history |
| PUT | `/coupons/{id}` | Update a coupon |
| GET | `/coupons/expiring-soon` | Get coupons expiring soon |

## Tech Stack

- Java 17
- Spring Boot 3.1.5
- Spring Security
- JWT
- PostgreSQL
- JPA/Hibernate
- Maven

## Database Design

**users** - User accounts and authentication
**coupons** - Coupon inventory with quantity and expiry tracking
**coupon_claims** - Records of which user claimed which coupon

Relationships between tables are managed using JPA entity mappings and foreign key constraints.

## Testing

This project was tested using Postman to verify authentication, coupon management, claiming, and duplicate claim prevention.
