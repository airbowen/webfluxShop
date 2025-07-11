# Store Order Management System

A comprehensive e-commerce order management system built with Spring Boot, featuring distributed architecture, event-driven messaging, and fault tolerance.

## 🎯 System Features

- **Multi-Product Orders**: Support for placing orders with multiple products in a single transaction
- **Automatic Order Splitting**: Orders are automatically split by merchant for asynchronous processing
- **JWT Authentication**: Unified authentication with userId and loginName claims
- **Event-Driven Architecture**: Kafka-based messaging for loose coupling and scalability
- **Distributed Tracing**: End-to-end request tracking and observability
- **Database Versioning**: Liquibase for schema management and version control
- **Caching & Concurrency**: Redis for distributed locking and idempotency
- **Fault Tolerance**: Message retry mechanism with Dead Letter Queue (DLQ)
- **Observability**: Centralized logging and metrics with Prometheus

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Order Service │    │   Kafka         │
│   (Client)      │◄──►│   (Spring Boot) │───►│   (Event Bus)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   MySQL         │
                       │   (Database)    │
                       └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Redis         │
                       │   (Cache/Lock)  │
                       └─────────────────┘
```

## 📊 Database Schema

### Core Tables

1. **user** - User information and authentication
2. **merchant** - Merchant/seller information
3. **product** - Product catalog with inventory
4. **order** - Order headers with merchant-specific sub-orders
5. **order_item** - Order line items
6. **order_message** - Kafka message tracking and retry

### Key Features

- **Optimistic Locking**: Version column in product table
- **Snowflake IDs**: Unique order codes for distributed systems
- **Foreign Key Constraints**: Data integrity across tables
- **Indexes**: Optimized queries for common access patterns

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Kafka 2.8+
- Docker (optional)

### Environment Setup

1. **Database Setup**
   ```sql
   CREATE DATABASE store_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Environment Variables**
   ```bash
   export DB_USERNAME=your_db_user
   export DB_PASSWORD=your_db_password
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   ```

### Running the Application

1. **Build the project**
   ```bash
   mvn clean install
   ```

2. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

3. **Verify startup**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## 📡 API Endpoints

### Order Management

#### Create Order
```http
POST /api/orders
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "items": [
    {
      "productId": 101,
      "quantity": 2
    },
    {
      "productId": 202,
      "quantity": 1
    }
  ]
}
```

#### Get Order by Code
```http
GET /api/orders/{orderCode}
Authorization: Bearer <jwt_token>
```

#### Get User Orders
```http
GET /api/orders
Authorization: Bearer <jwt_token>
```

## 🔄 Order Processing Flow

1. **Order Creation**
   - Validate request and products
   - Group items by merchant
   - Acquire distributed lock
   - Check idempotency
   - Update inventory with optimistic locking
   - Create order and order items
   - Send Kafka message
   - Release lock

2. **Message Processing**
   - Kafka producer sends order-created events
   - Failed messages stored in order_message table
   - Scheduled retry mechanism (every 5 minutes)
   - Dead Letter Queue after 3 retries

3. **Order Splitting**
   - Orders automatically split by merchant
   - Each merchant gets separate order
   - Independent processing and fulfillment

## 🛡️ Concurrency & Consistency

### Distributed Locking
- Redis-based locks prevent race conditions
- Lock timeout: 30 seconds
- Idempotency keys prevent duplicate orders

### Optimistic Locking
- Product inventory uses version column
- Concurrent updates detected and handled
- Retry mechanism for conflicts

### Transaction Management
- Database transactions ensure consistency
- Rollback on failures
- Eventual consistency for cross-service operations

## 📈 Monitoring & Observability

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Redis health
curl http://localhost:8080/actuator/health/redis
```

### Metrics
- Prometheus metrics available at `/actuator/prometheus`
- Custom metrics for order processing
- Kafka producer/consumer metrics

### Logging
- Structured logging with trace IDs
- Log levels configurable per package
- File and console output

## 🔧 Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
store:
  order:
    max-items-per-order: 50
    max-retry-count: 3
    retry-interval-minutes: 5
  redis:
    lock:
      timeout-seconds: 30
      idempotency-ttl-hours: 24
```

### Environment Variables

- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `REDIS_HOST` - Redis server host
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers
- `SERVER_PORT` - Application port (default: 8080)

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Manual Testing
```bash
# Create test order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer test.123.user1" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ]
  }'
```

## 🚨 Error Handling

### Retry Mechanism
- Failed Kafka messages retried automatically
- Configurable retry count and intervals
- Dead Letter Queue for permanently failed messages

### Exception Handling
- Global exception handler
- Proper HTTP status codes
- Detailed error messages for debugging

### Circuit Breaker
- Redis connection failures handled gracefully
- Database connection pooling
- Kafka producer resilience

## 🔐 Security

### JWT Authentication
- Token-based authentication
- User ID extraction from claims
- Authorization header validation

### Data Validation
- Input validation with Bean Validation
- SQL injection prevention
- XSS protection

## 📚 Additional Resources

### Database Migration
```bash
# View Liquibase status
mvn liquibase:status

# Update database schema
mvn liquibase:update
```

### Kafka Topics
- `order-created` - New order events
- `order-paid` - Payment confirmation events
- `order-cancelled` - Order cancellation events

### Redis Keys
- `order:lock:{userId}` - User order locks
- `order:idempotency:{userId}:{orderCode}` - Idempotency keys
- `stock:lock:{productId}` - Product inventory locks

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the logs for debugging information 