# Security Guide

This guide covers security best practices, configurations, and recommendations for deploying the Kafka Web Tool v2.0 in production environments.

## 🔒 Security Overview

### Security Features
- **Authentication**: JWT-based user authentication
- **Authorization**: Role-based access control (RBAC)
- **Encryption**: TLS/SSL for data in transit
- **Input Validation**: Protection against injection attacks
- **Audit Logging**: Comprehensive security event logging
- **Session Management**: Secure session handling
- **CORS Protection**: Cross-origin request security

## 🔐 Authentication & Authorization

### JWT Authentication
```yaml
# Backend configuration
security:
  jwt:
    secret: ${JWT_SECRET} # Use 256-bit secret
    expiration: 86400     # 24 hours
    refresh-expiration: 604800 # 7 days
    algorithm: HS256
```

### User Management
```sql
-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Create roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Full system access'),
('OPERATOR', 'Kafka operations access'),
('VIEWER', 'Read-only access');
```

### Role-Based Access Control
```java
// Controller security annotations
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/topics/{topicName}")
public ResponseEntity<String> deleteTopic(@PathVariable String topicName) {
    // Only admins can delete topics
}

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@PostMapping("/topics")
public ResponseEntity<String> createTopic(@RequestBody TopicRequest request) {
    // Admins and operators can create topics
}

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
@GetMapping("/topics")
public ResponseEntity<List<Topic>> getTopics() {
    // All authenticated users can view topics
}
```

## 🔑 Credential Management

### Environment Variables
```bash
# Use strong, unique passwords
DB_PASSWORD=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 32)
KAFKA_SASL_PASSWORD=your_secure_kafka_password

# Never hardcode credentials in configuration files
```

### Kubernetes Secrets
```yaml
# Create secrets for sensitive data
apiVersion: v1
kind: Secret
metadata:
  name: kafka-web-tool-secrets
  namespace: kafka-tool
type: Opaque
data:
  db-password: <base64-encoded-password>
  jwt-secret: <base64-encoded-secret>
  kafka-password: <base64-encoded-kafka-password>
```

### Secret Management Best Practices
```bash
# Use external secret management
# - HashiCorp Vault
# - AWS Secrets Manager
# - Azure Key Vault
# - Google Secret Manager

# Example with Vault
vault kv put secret/kafka-web-tool \
  db_password="secure_password" \
  jwt_secret="secure_jwt_secret"
```

## 🌐 Network Security

### TLS/SSL Configuration
```yaml
# Backend HTTPS configuration
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: kafka-web-tool
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
    client-auth: want
  port: 8443
```

### Nginx SSL Configuration
```nginx
server {
    listen 443 ssl http2;
    server_name kafkawebtool.yourdomain.com;
    
    # SSL certificates
    ssl_certificate /etc/ssl/certs/kafka-web-tool.crt;
    ssl_certificate_key /etc/ssl/private/kafka-web-tool.key;
    
    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Referrer-Policy strict-origin-when-cross-origin;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'";
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name kafkawebtool.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### Firewall Configuration
```bash
# Allow only necessary ports
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP (redirect to HTTPS)
ufw allow 443/tcp   # HTTPS
ufw deny 8080/tcp   # Block direct backend access
ufw deny 5432/tcp   # Block direct database access
ufw enable
```

## 🛡️ Application Security

### Input Validation
```java
// Backend validation
@RestController
@Validated
public class TopicController {
    
    @PostMapping("/topics")
    public ResponseEntity<String> createTopic(
            @Valid @RequestBody TopicRequest request) {
        // Validation annotations ensure input safety
    }
}

// Request validation
public class TopicRequest {
    @NotBlank(message = "Topic name is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid topic name")
    @Size(max = 255, message = "Topic name too long")
    private String name;
    
    @Min(value = 1, message = "Partitions must be at least 1")
    @Max(value = 1000, message = "Too many partitions")
    private int partitions;
}
```

### SQL Injection Prevention
```java
// Use parameterized queries
@Repository
public class ConnectionRepository {
    
    @Query("SELECT c FROM Connection c WHERE c.name = :name AND c.userId = :userId")
    Optional<Connection> findByNameAndUserId(@Param("name") String name, @Param("userId") UUID userId);
    
    // Never use string concatenation for queries
    // BAD: "SELECT * FROM connections WHERE name = '" + name + "'"
}
```

### XSS Prevention
```typescript
// Frontend XSS prevention
import DOMPurify from 'dompurify';

// Sanitize user input
const sanitizeInput = (input: string): string => {
  return DOMPurify.sanitize(input);
};

// Use React's built-in XSS protection
const MessageDisplay: React.FC<{ message: string }> = ({ message }) => {
  return <div>{message}</div>; // React automatically escapes
};
```

### CSRF Protection
```java
// Backend CSRF configuration
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/**") // Exclude auth endpoints
            );
        return http.build();
    }
}
```

## 🔍 Audit Logging

### Security Event Logging
```java
// Audit logging configuration
@Component
public class SecurityAuditLogger {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    public void logLoginAttempt(String username, String ipAddress, boolean success) {
        auditLogger.info("LOGIN_ATTEMPT: user={}, ip={}, success={}", 
            username, ipAddress, success);
    }
    
    public void logTopicAccess(String username, String topicName, String action) {
        auditLogger.info("TOPIC_ACCESS: user={}, topic={}, action={}", 
            username, topicName, action);
    }
    
    public void logConfigurationChange(String username, String component, String change) {
        auditLogger.warn("CONFIG_CHANGE: user={}, component={}, change={}", 
            username, component, change);
    }
}
```

### Log Configuration
```yaml
# Separate security logs
logging:
  level:
    SECURITY_AUDIT: INFO
  appender:
    security:
      type: RollingFileAppender
      file: /app/logs/security-audit.log
      pattern: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
      rolling-policy:
        max-file-size: 100MB
        max-history: 90
```

## 🔐 Kafka Security

### SASL/SSL Configuration
```java
// Kafka security configuration
@Configuration
public class KafkaSecurityConfig {
    
    @Bean
    public Map<String, Object> kafkaProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, 
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"" + username + "\" password=\"" + password + "\";");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        return props;
    }
}
```

### ACL Configuration
```bash
# Kafka ACLs for the application
kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
  --add --allow-principal User:kafka-web-tool \
  --operation Read --operation Write --operation Create --operation Delete \
  --topic "*"

kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
  --add --allow-principal User:kafka-web-tool \
  --operation Read --operation Write \
  --group "*"
```

## 🏗️ Infrastructure Security

### Kubernetes Security
```yaml
# Pod Security Policy
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: kafka-web-tool-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### Network Policies
```yaml
# Restrict network access
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: kafka-web-tool-netpol
  namespace: kafka-tool
spec:
  podSelector:
    matchLabels:
      app: kafka-web-app-backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: kafka-web-app-frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to: []
    ports:
    - protocol: TCP
      port: 9092  # Kafka
```

## 🔒 Database Security

### Connection Security
```yaml
# Secure database connection
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/kafka_web_tool?sslmode=require&sslcert=/path/to/client-cert.pem&sslkey=/path/to/client-key.pem&sslrootcert=/path/to/ca-cert.pem
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

### Database Encryption
```sql
-- Enable encryption at rest
ALTER SYSTEM SET ssl = on;
ALTER SYSTEM SET ssl_cert_file = 'server.crt';
ALTER SYSTEM SET ssl_key_file = 'server.key';
ALTER SYSTEM SET ssl_ca_file = 'ca.crt';

-- Encrypt sensitive columns
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Store encrypted passwords
UPDATE users SET password_hash = crypt(password_hash, gen_salt('bf', 12));
```

## 🚨 Security Monitoring

### Security Metrics
```java
// Security metrics
@Component
public class SecurityMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter loginAttempts;
    private final Counter failedLogins;
    
    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.loginAttempts = Counter.builder("security.login.attempts")
            .description("Total login attempts")
            .register(meterRegistry);
        this.failedLogins = Counter.builder("security.login.failures")
            .description("Failed login attempts")
            .register(meterRegistry);
    }
    
    public void recordLoginAttempt(boolean success) {
        loginAttempts.increment();
        if (!success) {
            failedLogins.increment();
        }
    }
}
```

### Alerting Rules
```yaml
# Prometheus alerting rules
groups:
- name: security
  rules:
  - alert: HighFailedLoginRate
    expr: rate(security_login_failures_total[5m]) > 0.1
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: High failed login rate detected
      
  - alert: UnauthorizedAccess
    expr: rate(http_requests_total{status=~"401|403"}[5m]) > 0.05
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: Potential unauthorized access attempt
```

## ✅ Security Checklist

### Pre-deployment Security
- [ ] Strong passwords and secrets generated
- [ ] TLS certificates obtained and configured
- [ ] Database encryption enabled
- [ ] Firewall rules configured
- [ ] Network policies applied
- [ ] Security scanning completed
- [ ] Penetration testing performed

### Runtime Security
- [ ] Authentication enabled and tested
- [ ] Authorization rules verified
- [ ] Audit logging configured
- [ ] Security monitoring active
- [ ] Backup encryption verified
- [ ] Incident response plan ready

### Ongoing Security
- [ ] Regular security updates applied
- [ ] Log monitoring active
- [ ] Access reviews conducted
- [ ] Security metrics monitored
- [ ] Vulnerability scans scheduled
- [ ] Security training completed

---

**Next Steps:**
- [Monitoring Guide](MONITORING.md) - Set up security monitoring
- [Troubleshooting](TROUBLESHOOTING.md) - Resolve security issues
- [Backup Guide](BACKUP.md) - Secure backup procedures
