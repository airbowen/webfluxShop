# Store Order Management System - Deployment Guide

This guide provides step-by-step instructions for deploying the Store Order Management System.

## 🚀 Quick Start

### Prerequisites

- **Docker & Docker Compose**: [Install Docker](https://docs.docker.com/get-docker/)
- **Java 17+**: [Install OpenJDK 17](https://adoptium.net/)
- **Maven 3.6+**: [Install Maven](https://maven.apache.org/install.html)
- **Git**: [Install Git](https://git-scm.com/)

### 1. Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd store

# Build the application
mvn clean install
```

### 2. Start Infrastructure

```bash
# Start all infrastructure services
./scripts/start-infrastructure.sh
```

This will start:
- MySQL 8.0 (Database)
- Redis 7 (Cache & Distributed Locking)
- Kafka 7.4 (Event Streaming)
- Zookeeper (Kafka dependency)
- Prometheus (Metrics)
- Grafana (Visualization)
- Kafka UI (Kafka monitoring)

### 3. Configure Environment

```bash
# Set environment variables
export DB_USERNAME=store_user
export DB_PASSWORD=store_password
export JWT_SECRET=your-very-secure-jwt-secret-key-here
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### 4. Run the Application

```bash
# Start the Spring Boot application
mvn spring-boot:run
```

### 5. Test the System

```bash
# Run comprehensive API tests
./scripts/test-api.sh
```

## 📊 Monitoring & Observability

### Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Application | http://localhost:8080 | N/A |
| Health Check | http://localhost:8080/actuator/health | N/A |
| Prometheus | http://localhost:9090 | N/A |
| Grafana | http://localhost:3000 | admin/admin |
| Kafka UI | http://localhost:8080 | N/A |

### Key Metrics

- **Order Processing Rate**: Orders per minute
- **Payment Success Rate**: Successful payments percentage
- **Response Time**: API response times
- **Error Rate**: Failed requests percentage
- **Kafka Lag**: Message processing delays

## 🔧 Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
store:
  jwt:
    secret: ${JWT_SECRET:default-secret}
    expiration: ${JWT_EXPIRATION:86400000}
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

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | Database username | `store_user` |
| `DB_PASSWORD` | Database password | `store_password` |
| `JWT_SECRET` | JWT signing secret | `default-secret` |
| `REDIS_HOST` | Redis server host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `SERVER_PORT` | Application port | `8080` |

## 🧪 Testing

### Unit Tests

```bash
# Run unit tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### Integration Tests

```bash
# Run integration tests
mvn test -Dtest=*IntegrationTest

# Run with test profile
mvn test -Dspring.profiles.active=test
```

### API Tests

```bash
# Run API tests
./scripts/test-api.sh

# Manual testing with curl
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2}
    ]
  }'
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Database Connection Failed

```bash
# Check MySQL status
docker-compose ps mysql

# Check logs
docker-compose logs mysql

# Restart MySQL
docker-compose restart mysql
```

#### 2. Redis Connection Failed

```bash
# Check Redis status
docker-compose ps redis

# Test Redis connection
docker-compose exec redis redis-cli ping
```

#### 3. Kafka Connection Failed

```bash
# Check Kafka status
docker-compose ps kafka

# Check Kafka topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

#### 4. Application Won't Start

```bash
# Check application logs
tail -f logs/store-order-service.log

# Check health endpoint
curl http://localhost:8080/actuator/health

# Verify environment variables
echo $DB_USERNAME
echo $JWT_SECRET
```

### Log Analysis

```bash
# View application logs
tail -f logs/store-order-service.log

# Search for errors
grep -i error logs/store-order-service.log

# Search for specific order
grep "ORD123456789" logs/store-order-service.log
```

## 🔒 Security

### JWT Configuration

1. **Generate a secure secret**:
   ```bash
   openssl rand -base64 32
   ```

2. **Set environment variable**:
   ```bash
   export JWT_SECRET=your-generated-secret
   ```

3. **Validate token**:
   ```bash
   # Test JWT token validation
   curl -H "Authorization: Bearer invalid-token" \
        http://localhost:8080/api/orders
   ```

### Database Security

1. **Change default passwords**:
   ```sql
   ALTER USER 'store_user'@'%' IDENTIFIED BY 'new-secure-password';
   FLUSH PRIVILEGES;
   ```

2. **Restrict network access**:
   ```yaml
   # In docker-compose.yml
   mysql:
     ports:
       - "127.0.0.1:3306:3306"  # Only localhost access
   ```

## 📈 Performance Tuning

### Database Optimization

```sql
-- Add indexes for better performance
CREATE INDEX idx_order_user_status ON `order`(user_id, status);
CREATE INDEX idx_order_merchant_status ON `order`(merchant_id, status);
CREATE INDEX idx_product_merchant_status ON product(merchant_id, status);
```

### JVM Tuning

```bash
# Set JVM options for production
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run with optimized settings
mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
```

### Redis Optimization

```yaml
# In docker-compose.yml
redis:
  command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
```

## 🚀 Production Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t store-order-service .

# Run with Docker
docker run -d \
  --name store-order-service \
  -p 8080:8080 \
  -e DB_USERNAME=store_user \
  -e DB_PASSWORD=store_password \
  -e JWT_SECRET=your-secret \
  store-order-service
```

### Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: store-order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: store-order-service
  template:
    metadata:
      labels:
        app: store-order-service
    spec:
      containers:
      - name: store-order-service
        image: store-order-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Documentation](https://redis.io/documentation)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

## 🆘 Support

For issues and questions:

1. Check the troubleshooting section above
2. Review application logs
3. Check service health endpoints
4. Create an issue in the repository
5. Contact the development team

## 📝 Changelog

### Version 1.0.0
- Initial release
- Order management with merchant splitting
- Payment processing
- Kafka event streaming
- Redis distributed locking
- Comprehensive monitoring
- JWT authentication
- Fault tolerance with retry mechanism 