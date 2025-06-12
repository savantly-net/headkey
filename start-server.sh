#!/bin/bash

# Start HeadKey Application Server
# This script starts the Quarkus application in development mode

set -e

echo "🚀 Starting HeadKey Application Server..."

# Check if PostgreSQL is running
echo "📊 Starting PostgreSQL..."
docker compose up -d

# Wait a moment for PostgreSQL to be ready
sleep 2

echo "🔧 Starting Quarkus application in development mode..."
echo "💡 The application will be available at: http://localhost:8080"
echo "📋 API documentation at: http://localhost:8080/swagger-ui"
echo "🏥 Health check at: http://localhost:8080/api/v1/memory/health"
echo ""
echo "⏸️  Press Ctrl+C to stop the server"
echo ""

# Start the Quarkus application
./gradlew rest:quarkusDev --no-daemon