#!/bin/bash

# Store Order Management System - Infrastructure Startup Script

echo "🚀 Starting Store Order Management Infrastructure..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p logs
mkdir -p docker/prometheus
mkdir -p docker/grafana/provisioning/datasources
mkdir -p docker/grafana/provisioning/dashboards

# Start infrastructure services
echo "🐳 Starting Docker Compose services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "🔍 Checking service health..."

# Check MySQL
if docker-compose exec -T mysql mysqladmin ping -h localhost --silent; then
    echo "✅ MySQL is ready"
else
    echo "❌ MySQL is not ready"
fi

# Check Redis
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
fi

# Check Kafka
if docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
    echo "✅ Kafka is ready"
else
    echo "❌ Kafka is not ready"
fi

# Check Prometheus
if curl -s http://localhost:9090/-/healthy > /dev/null; then
    echo "✅ Prometheus is ready"
else
    echo "❌ Prometheus is not ready"
fi

# Check Grafana
if curl -s http://localhost:3000/api/health > /dev/null; then
    echo "✅ Grafana is ready"
else
    echo "❌ Grafana is not ready"
fi

echo ""
echo "🎉 Infrastructure is ready!"
echo ""
echo "📊 Service URLs:"
echo "   - Application: http://localhost:8080"
echo "   - Kafka UI: http://localhost:8080 (Kafka UI)"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "🔧 Database Connection:"
echo "   - Host: localhost"
echo "   - Port: 3306"
echo "   - Database: store_db"
echo "   - Username: store_user"
echo "   - Password: store_password"
echo ""
echo "📝 Next steps:"
echo "   1. Set environment variables:"
echo "      export DB_USERNAME=store_user"
echo "      export DB_PASSWORD=store_password"
echo "      export JWT_SECRET=your-secure-jwt-secret"
echo "   2. Run the application: mvn spring-boot:run"
echo "   3. Test the API endpoints"
echo ""
echo "🛑 To stop services: docker-compose down" 