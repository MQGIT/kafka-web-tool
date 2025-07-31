# Configuration Guide

This guide covers all configuration options for the Kafka Web Tool v2.0, including environment variables, application properties, and runtime settings.

## 🔧 Environment Variables

### Database Configuration
```bash
# PostgreSQL Database
DB_HOST=localhost                    # Database host
DB_PORT=5432                        # Database port
DB_NAME=kafka_web_tool              # Database name
DB_USERNAME=postgres                # Database username
DB_PASSWORD=your_secure_password    # Database password
DB_SSL_MODE=require                 # SSL mode (disable|allow|prefer|require)
DB_MAX_CONNECTIONS=20               # Maximum connection pool size
DB_MIN_CONNECTIONS=5                # Minimum connection pool size
```

### Application Configuration
```bash
# Server Settings
SERVER_PORT=8080                    # Backend server port
SERVER_ADDRESS=0.0.0.0             # Server bind address
CONTEXT_PATH=/api/v1               # API context path

# Frontend Settings
FRONTEND_PORT=3000                  # Frontend development port
FRONTEND_BUILD_PATH=../frontend/dist # Frontend build directory

# Logging
LOG_LEVEL=INFO                      # Log level (TRACE|DEBUG|INFO|WARN|ERROR)
LOG_FILE_PATH=/app/logs            # Log file directory
LOG_MAX_FILE_SIZE=100MB            # Maximum log file size
LOG_MAX_HISTORY=30                 # Log retention days
```

### Security Configuration
```bash
# Authentication
AUTH_ENABLED=true                   # Enable authentication
AUTH_JWT_SECRET=your_jwt_secret     # JWT signing secret
AUTH_JWT_EXPIRATION=86400          # JWT expiration (seconds)
AUTH_SESSION_TIMEOUT=3600          # Session timeout (seconds)

# HTTPS/TLS
ENABLE_HTTPS=true                   # Enable HTTPS
SSL_CERT_PATH=/path/to/cert.pem    # SSL certificate path
SSL_KEY_PATH=/path/to/key.pem      # SSL private key path
SSL_TRUST_STORE=/path/to/trust.jks # Trust store path
SSL_TRUST_STORE_PASSWORD=password   # Trust store password
```

### Kafka Configuration
```bash
# Default Kafka Settings (Optional)
DEFAULT_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
DEFAULT_KAFKA_SECURITY_PROTOCOL=PLAINTEXT
DEFAULT_KAFKA_SASL_MECHANISM=PLAIN
DEFAULT_KAFKA_SASL_USERNAME=
DEFAULT_KAFKA_SASL_PASSWORD=

# Consumer Settings
CONSUMER_SESSION_TIMEOUT_MS=30000   # Consumer session timeout
CONSUMER_MAX_POLL_RECORDS=500      # Maximum records per poll
CONSUMER_AUTO_COMMIT_INTERVAL=5000 # Auto commit interval
CONSUMER_DEFAULT_TIMEOUT_MS=30000  # Default consumer timeout
```

### Performance Configuration
```bash
# Thread Pool Settings
THREAD_POOL_CORE_SIZE=10           # Core thread pool size
THREAD_POOL_MAX_SIZE=50            # Maximum thread pool size
THREAD_POOL_QUEUE_CAPACITY=100     # Thread pool queue capacity

# Cache Settings
CACHE_ENABLED=true                  # Enable caching
CACHE_TTL_SECONDS=300              # Cache time-to-live
CACHE_MAX_SIZE=1000                # Maximum cache entries

# Connection Pool
HTTP_CONNECTION_TIMEOUT=5000        # HTTP connection timeout (ms)
HTTP_READ_TIMEOUT=30000            # HTTP read timeout (ms)
HTTP_MAX_CONNECTIONS=100           # Maximum HTTP connections
```

## 📄 Application Properties

### Backend Configuration (application.yml)
```yaml
spring:
  application:
    name: kafka-web-tool-backend
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kafka_web_tool}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_MAX_CONNECTIONS:20}
      minimum-idle: ${DB_MIN_CONNECTIONS:5}
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  security:
    enabled: ${AUTH_ENABLED:true}
    jwt:
      secret: ${AUTH_JWT_SECRET:default_secret}
      expiration: ${AUTH_JWT_EXPIRATION:86400}
  
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: ${SERVER_PORT:8080}
  address: ${SERVER_ADDRESS:0.0.0.0}
  servlet:
    context-path: ${CONTEXT_PATH:/api/v1}
  ssl:
    enabled: ${ENABLE_HTTPS:false}
    key-store: ${SSL_CERT_PATH:}
    key-store-password: ${SSL_KEY_PASSWORD:}
    trust-store: ${SSL_TRUST_STORE:}
    trust-store-password: ${SSL_TRUST_STORE_PASSWORD:}

logging:
  level:
    org.marsem.kafka: ${LOG_LEVEL:INFO}
    org.springframework.kafka: WARN
    org.apache.kafka: WARN
  file:
    path: ${LOG_FILE_PATH:/app/logs}
  logback:
    rollingpolicy:
      max-file-size: ${LOG_MAX_FILE_SIZE:100MB}
      max-history: ${LOG_MAX_HISTORY:30}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

kafka:
  consumer:
    session-timeout-ms: ${CONSUMER_SESSION_TIMEOUT_MS:30000}
    max-poll-records: ${CONSUMER_MAX_POLL_RECORDS:500}
    auto-commit-interval-ms: ${CONSUMER_AUTO_COMMIT_INTERVAL:5000}
    default-timeout-ms: ${CONSUMER_DEFAULT_TIMEOUT_MS:30000}
  
  producer:
    batch-size: 16384
    linger-ms: 5
    buffer-memory: 33554432
    acks: all
    retries: 3
```

### Frontend Configuration (vite.config.ts)
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: parseInt(process.env.FRONTEND_PORT || '3000'),
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.SERVER_PORT || '8080'}`,
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: process.env.NODE_ENV === 'development',
    minify: process.env.NODE_ENV === 'production',
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          antd: ['antd'],
          charts: ['chart.js', 'react-chartjs-2']
        }
      }
    }
  },
  define: {
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development')
  }
})
```

## 🐳 Docker Configuration

### Backend Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy application jar
COPY target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile
```dockerfile
FROM nginx:alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built application
COPY dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set ownership
RUN chown -R nginx:nginx /usr/share/nginx/html

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### Nginx Configuration
```nginx
server {
    listen 80;
    server_name localhost;
    
    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Referrer-Policy strict-origin-when-cross-origin;
    
    # Frontend routes
    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }
    
    # API proxy
    location /api/ {
        proxy_pass http://kafka-web-app-backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Buffer settings
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # Static assets caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

## ☸️ Kubernetes Configuration

### ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-web-tool-config
  namespace: kafka-tool
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/kafka_web_tool
        username: postgres
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      jpa:
        hibernate:
          ddl-auto: update
        show-sql: false
    
    server:
      port: 8080
    
    logging:
      level:
        org.marsem.kafka: INFO
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
```

### Resource Limits
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

## 🔒 Security Configuration

### JWT Configuration
```yaml
security:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret}
    expiration: 86400 # 24 hours
    refresh-expiration: 604800 # 7 days
```

### CORS Configuration
```yaml
cors:
  allowed-origins:
    - "https://kafkawebtool.yourdomain.com"
    - "http://localhost:3000"
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
    - OPTIONS
  allowed-headers:
    - "*"
  allow-credentials: true
```

## 📊 Monitoring Configuration

### Prometheus Metrics
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: kafka-web-tool
      environment: ${ENVIRONMENT:production}
```

### Health Check Configuration
```yaml
management:
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    db:
      enabled: true
    kafka:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB
```

## 🎛️ Runtime Configuration

### JVM Options
```bash
# Memory settings
JAVA_OPTS="-Xms512m -Xmx1g"

# Garbage collection
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Monitoring
JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# Security
JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
```

### Environment-Specific Configurations

#### Development
```bash
LOG_LEVEL=DEBUG
AUTH_ENABLED=false
CACHE_ENABLED=false
DB_SSL_MODE=disable
```

#### Staging
```bash
LOG_LEVEL=INFO
AUTH_ENABLED=true
CACHE_ENABLED=true
DB_SSL_MODE=prefer
```

#### Production
```bash
LOG_LEVEL=WARN
AUTH_ENABLED=true
CACHE_ENABLED=true
DB_SSL_MODE=require
ENABLE_HTTPS=true
```

---

**Next Steps:**
- [Security Guide](SECURITY.md) - Secure your configuration
- [Troubleshooting](TROUBLESHOOTING.md) - Resolve configuration issues
- [Monitoring](MONITORING.md) - Monitor your configuration
