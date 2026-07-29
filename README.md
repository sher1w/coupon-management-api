# Coupon Management API

A Spring Boot REST API for managing coupons with JWT authentication and race condition prevention.

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL
- Maven

### Setup

1. **Create database:**
```bash
createdb coupon_db
```

2. **Build & Run:**
```bash
mvn clean install
mvn spring-boot:run
```

API runs on: `http://localhost:8080/api`

## Testing the API

### 1. Register User
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

Copy the `token` from response.

### 2. Create Coupon
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

### 3. Claim Coupon
```bash
curl -X POST http://localhost:8080/api/coupons/claim \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "couponCode": "SUMMER2024"
  }'
```

Expected: `"success": true`

### 4. Claim Again (Should Fail)
Run the same command as step 3.

Expected: `"success": false, "message": "You have already claimed this coupon"`

This proves **race condition prevention works**.

### 5. Get My Coupons
```bash
curl -X GET http://localhost:8080/api/coupons/my-coupons \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## How Race Conditions Are Prevented

When multiple users claim the same coupon simultaneously, the system uses:

1. **Pessimistic Write Locking** - Database-level lock ensures only one user can claim at a time
2. **SERIALIZABLE Isolation** - Transactions execute sequentially, not in parallel
3. **Atomic Transactions** - All operations succeed or all rollback together

Result: If 100 users claim 10 coupons, exactly 10 succeed. No lost updates, no data inconsistency.

## API Endpoints

- `POST /auth/register` - Register user
- `POST /auth/login` - Login user
- `POST /coupons/create` - Create coupon
- `GET /coupons/available` - List available coupons
- `POST /coupons/claim` - Claim coupon
- `GET /coupons/my-coupons` - Get claimed coupons
- `GET /coupons/my-claims` - Claim history
- `PUT /coupons/{id}` - Update coupon
- `GET /coupons/expiring-soon` - Coupons expiring in 7 days

## Tech Stack

- Spring Boot 3.1.5
- Spring Security + JWT
- PostgreSQL
- JPA/Hibernate
- Maven

## Database Schema

**Users Table:** Stores user accounts with hashed passwords

**Coupons Table:** Stores coupons with quantity tracking and expiry dates

**CouponClaims Table:** Tracks which user claimed which coupon with timestamp

Proper indexes ensure efficient queries. Foreign keys maintain data integrity.
