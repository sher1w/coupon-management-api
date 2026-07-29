# Coupon Management API

An asset management system for coupons/vouchers built with Spring Boot, demonstrating **data integrity** and **concurrency handling** in production-grade applications.

## 🎯 Key Features

✅ **JWT Authentication** - Secure user registration and login  
✅ **Concurrency Control** - Pessimistic locking to prevent race conditions  
✅ **Transaction Management** - SERIALIZABLE isolation for data consistency  
✅ **Efficient Queries** - JOINs and proper indexing for performance  
✅ **RESTful API** - Clean endpoints with proper HTTP status codes  

---

## 📋 Prerequisites

- Java 17+
- PostgreSQL 12+
- Maven 3.8+

---

## 🚀 Setup & Installation

### 1. Clone & Build

```bash
git clone <your-repo-url>
cd coupon-management-api
mvn clean install
```

### 2. Database Setup

Create PostgreSQL database:

```sql
CREATE DATABASE coupon_db;
```

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/coupon_db
    username: postgres
    password: your_password
```

### 3. Run Application

```bash
mvn spring-boot:run
```

API runs on: **http://localhost:8080/api**

---

## 🔐 Authentication

All endpoints (except `/auth/**`) require JWT token in header:

```
Authorization: Bearer <jwt_token>
```

---

## 📚 API Endpoints

### Authentication

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
{
  "token": "eyJhbGc...",
  "userId": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "message": "User registered successfully"
}
```

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response: 200 OK
{
  "token": "eyJhbGc...",
  "userId": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "message": "Login successful"
}
```

---

### Coupon Operations

#### Create Coupon (Admin)
```
POST /api/coupons/create
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "SUMMER2024",
  "discountValue": 20.00,
  "discountType": "PERCENTAGE",
  "totalQuantity": 100,
  "expiryDate": "2024-08-31T23:59:59"
}

Response: 201 Created
{
  "id": 1,
  "code": "SUMMER2024",
  "discountValue": 20.00,
  "discountType": "PERCENTAGE",
  "totalQuantity": 100,
  "claimedQuantity": 0,
  "remainingQuantity": 100,
  "expiryDate": "2024-08-31T23:59:59",
  "isActive": true,
  "createdAt": "2024-07-27T10:00:00",
  "updatedAt": "2024-07-27T10:00:00"
}
```

#### Get Available Coupons
```
GET /api/coupons/available
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "code": "SUMMER2024",
    "discountValue": 20.00,
    "discountType": "PERCENTAGE",
    "remainingQuantity": 85,
    ...
  }
]
```

#### Claim Coupon (CRITICAL - Handles Race Conditions)
```
POST /api/coupons/claim
Authorization: Bearer <token>
Content-Type: application/json

{
  "couponCode": "SUMMER2024"
}

Response: 200 OK
{
  "claimId": 5,
  "couponId": 1,
  "couponCode": "SUMMER2024",
  "discountValue": 20.00,
  "discountType": "PERCENTAGE",
  "claimedAt": "2024-07-27T10:15:32",
  "success": true,
  "message": "Coupon claimed successfully"
}

Response: 400 Bad Request (if out of stock)
{
  "couponId": 1,
  "couponCode": "SUMMER2024",
  "success": false,
  "message": "Coupon is out of stock"
}
```

#### Get My Claimed Coupons
```
GET /api/coupons/my-coupons
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "code": "SUMMER2024",
    "discountValue": 20.00,
    ...
  }
]
```

#### Get Claim History (Paginated)
```
GET /api/coupons/my-claims?page=0&size=10
Authorization: Bearer <token>

Response: 200 OK
{
  "content": [...],
  "pageable": {...},
  "totalElements": 15,
  "totalPages": 2
}
```

#### Get Coupons Expiring Soon
```
GET /api/coupons/expiring-soon
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 2,
    "code": "FLASH",
    "expiryDate": "2024-08-02T23:59:59",
    ...
  }
]
```

#### Update Coupon
```
PUT /api/coupons/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "totalQuantity": 200,
  "expiryDate": "2024-09-30T23:59:59",
  "isActive": true
}

Response: 200 OK
{
  "id": 1,
  "code": "SUMMER2024",
  "totalQuantity": 200,
  ...
}
```

---

## 🔒 Concurrency Handling (Race Condition Prevention)

### Problem
When 100+ users try to claim the same coupon simultaneously, a race condition occurs:

```
User A: Read claimedQuantity = 99/100
User B: Read claimedQuantity = 99/100  [same value!]
User A: Increment to 100, save
User B: Increment to 100, save           [OVERWRITE! Lost update]
Result: Both claimed it, but count only increased by 1
```

### Solution: Pessimistic Write Locking + SERIALIZABLE Isolation

In `CouponService.claimCoupon()`:

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public CouponDTO.ClaimResponse claimCoupon(Long userId, CouponDTO.ClaimRequest request) {
    // LOCK: Only one thread can read/modify the coupon at a time
    Coupon coupon = couponRepository.findByCodeWithLock(request.getCouponCode())
        .orElseThrow(...);
    
    // Check all conditions inside transaction (atomic operation)
    if (!coupon.isAvailableForClaim()) return error;
    if (claimRepository.hasUserClaimedCoupon(...)) return error;
    
    // Increment and save (within same transaction = atomic)
    coupon.incrementClaimedQuantity();
    couponRepository.save(coupon);
    claimRepository.save(claim);
    
    // Transaction commits only if all above succeed
}
```

**How it prevents race conditions:**

1. **Pessimistic Lock** (`findByCodeWithLock`): Database-level row lock  
   - Only one thread can hold the lock
   - Other threads WAIT for lock release

2. **SERIALIZABLE Isolation**: Strictest isolation level  
   - Transactions execute sequentially (not parallel)
   - Prevents phantom reads and dirty reads

3. **Single Transaction**: All operations (read, check, increment, save) are atomic  
   - Either ALL succeed or ALL rollback
   - No partial updates possible

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### Coupons Table
```sql
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_value NUMERIC(10,2) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    total_quantity INTEGER NOT NULL,
    claimed_quantity INTEGER DEFAULT 0,
    expiry_date TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    version BIGINT,  -- For optimistic locking
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    INDEX idx_code (code),
    INDEX idx_status (is_active),
    INDEX idx_expiry (expiry_date)
);
```

### CouponClaims Table (JOIN)
```sql
CREATE TABLE coupon_claims (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    
    UNIQUE (user_id, coupon_id),  -- Prevent duplicate claims
    INDEX idx_user (user_id),
    INDEX idx_coupon (coupon_id),
    INDEX idx_user_coupon (user_id, coupon_id),
    INDEX idx_claimed_at (claimed_at)
);
```

---

## 🧪 Testing Concurrent Claims

### Load Test Script

```bash
#!/bin/bash

# Register user
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test'$RANDOM'@example.com",
    "password": "pass123",
    "firstName": "Test",
    "lastName": "User"
  }' | jq -r '.token')

echo "Token: $TOKEN"

# Attempt to claim same coupon 10 times concurrently
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/coupons/claim \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"couponCode": "SUMMER2024"}' &
done

wait
```

**Expected behavior:** Only 1 succeeds, others get "already claimed" error.

---

## 📝 Project Structure

```
coupon-management-api/
├── src/main/java/com/luarc/
│   ├── entity/           # JPA entities (User, Coupon, CouponClaim)
│   ├── repository/       # Data access layer
│   ├── service/          # Business logic (transaction handling)
│   ├── controller/       # REST endpoints
│   ├── dto/              # Request/Response objects
│   ├── security/         # JWT, authentication, authorization
│   ├── config/           # Spring Security config
│   └── CouponManagementApplication.java
├── src/main/resources/
│   └── application.yml   # Configuration
├── pom.xml               # Maven dependencies
└── README.md             # This file
```

---

## 🎬 Video Explanation (For Interview)

Focus on these points when recording your 1-2 min video:

### Background (15 seconds)
"Hi, I'm Sherwin, a final-year Computer Engineering student at Goa College of Engineering. I'm looking for backend engineering roles where I can work on scalable systems."

### Problem (20 seconds)
"For this project, the main challenge was preventing race conditions. Imagine 100 users trying to claim the last coupon simultaneously—without proper locking, multiple users might successfully claim the same coupon."

### Solution (45 seconds)
"I solved this with three strategies:

1. **Pessimistic Locking**: Database-level row locks ensure only one thread modifies a coupon at a time.

2. **SERIALIZABLE Isolation**: Strictest SQL isolation level—transactions execute sequentially, not in parallel.

3. **Single Transaction**: All operations (read quantity, check conditions, increment, save) happen atomically within one transaction. If any step fails, entire transaction rolls back.

The code uses Spring Data JPA's `@Lock(PESSIMISTIC_WRITE)` annotation and `@Transactional(isolation = SERIALIZABLE)` to implement this."

### Key Technical Details (30 seconds)
"Other important aspects:
- JWT authentication with Spring Security
- Efficient queries using JOINs and database indexes
- Proper transaction boundaries to avoid deadlocks
- Clean separation of concerns (entities → repositories → services → controllers)"

---

## 🚦 What Reviewers Will Look For

✅ **Race condition handling** - They'll test with concurrent requests  
✅ **Clean code** - Easy to understand and maintain  
✅ **Proper use of transactions** - No data inconsistency  
✅ **Security** - JWT auth, password hashing, input validation  
✅ **Error handling** - Meaningful error messages  
✅ **Database design** - Proper indexes and relationships  
✅ **Scalability** - Handles high concurrent load  

---

## 📝 Notes for Your Interview

**What YOU should explain:**

1. Why pessimistic locking? "Because I need GUARANTEED consistency, not eventual consistency."
2. Why SERIALIZABLE? "It's strict but ensures no race conditions can occur."
3. The risk? "Performance impact—each claim takes longer because of serialization. But for coupons, consistency is more important than speed."
4. Alternative approaches? "Optimistic locking using @Version field, but that requires retry logic."

---

## 🔧 Common Issues & Solutions

**Port 8080 already in use?**
```bash
# Change in application.yml
server:
  port: 8081
```

**Database connection refused?**
```bash
# Start PostgreSQL
# Ubuntu/Debian:
sudo systemctl start postgresql

# macOS:
brew services start postgresql
```

**JWT token expired?**
- Tokens expire after 24 hours (configurable in `application.yml`)
- Login again to get a new token

---

## 📞 Support

For questions during implementation, refer to:
- `CouponService.claimCoupon()` - Core concurrency logic
- `CouponRepository.findByCodeWithLock()` - Pessimistic lock query
- `SecurityConfig` - JWT setup

---

## ⭐ This Project Demonstrates

- ✅ Production-grade Spring Boot application
- ✅ Proper transaction management and isolation levels
- ✅ Race condition prevention techniques
- ✅ RESTful API design
- ✅ JWT authentication
- ✅ Database indexing and query optimization
- ✅ Clean code architecture
- ✅ Error handling and validation

**Good luck with your interview! 🚀**
