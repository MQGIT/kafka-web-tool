# Kafka Web App v2 - Deployment Guide

This guide covers deploying the Kafka Web App v2 to Kubernetes in the `kafka-tool` namespace with the hostname `kafkawebtool.marsem.org`.

## Prerequisites

### Required Tools
- **kubectl** - Kubernetes command-line tool
- **Docker** - Container runtime
- **Maven** - Java build tool (for building from source)

### Kubernetes Cluster Requirements
- Kubernetes 1.20+
- NGINX Ingress Controller
- cert-manager (optional, for SSL certificates)
- Persistent Volume support
- At least 4GB RAM and 2 CPU cores available

### External Dependencies
- **Kafka Cluster** - Update `KAFKA_BOOTSTRAP_SERVERS` in deployment
- **DNS Configuration** - Point `kafkawebtool.marsem.org` to your ingress

## Quick Deployment

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd confluent/kafka-web-app-v2
```

### 2. Configure Environment
Edit the following files to match your environment:

**k8s/configmap.yaml:**
```yaml
# Update Kafka bootstrap servers
KAFKA_BOOTSTRAP_SERVERS: "your-kafka-cluster:9092"
```

**k8s/secret.yaml:**
```yaml
# Update database credentials (base64 encoded)
DB_USERNAME: <base64-encoded-username>
DB_PASSWORD: <base64-encoded-password>
```

**deploy.sh:**
```bash
# Update container registry if using one
REGISTRY="your-registry.com"
```

### 3. Deploy
```bash
# Full deployment
./deploy.sh deploy

# Or step by step:
./deploy.sh build    # Build only
./deploy.sh deploy   # Deploy to Kubernetes
./deploy.sh status   # Check status
```

## Manual Deployment

### 1. Build Application
```bash
cd backend
mvn clean package -DskipTests
docker build -t kafka-web-app-v2:latest .
```

### 2. Create Namespace
```bash
kubectl apply -f k8s/namespace.yaml
```

### 3. Deploy Infrastructure
```bash
# Configuration and secrets
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/nginx-config.yaml

# Database and cache
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml

# Wait for infrastructure
kubectl wait --for=condition=ready pod -l app=postgres -n kafka-tool --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n kafka-tool --timeout=300s
```

### 4. Deploy Application
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/ingress.yaml

# Wait for application
kubectl wait --for=condition=available deployment/kafka-web-app-backend -n kafka-tool --timeout=600s
kubectl wait --for=condition=available deployment/kafka-web-app-frontend -n kafka-tool --timeout=300s
```

## Configuration

### Environment Variables
Key environment variables that can be customized:

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka cluster endpoints | `localhost:9092` |
| `DB_USERNAME` | Database username | `kafka_user` |
| `DB_PASSWORD` | Database password | `kafka_password` |
| `REDIS_HOST` | Redis hostname | `redis` |
| `JWT_SECRET` | JWT signing secret | (generated) |

### Kafka Connection
Update the Kafka bootstrap servers in `k8s/configmap.yaml`:
```yaml
KAFKA_BOOTSTRAP_SERVERS: "broker1:9092,broker2:9092,broker3:9092"
```

For secure Kafka clusters, you'll need to configure additional security settings through the application's connection management interface.

### SSL/TLS Configuration
The ingress is configured for SSL with Let's Encrypt. Ensure:
1. cert-manager is installed in your cluster
2. DNS points to your ingress controller
3. Update the email in `k8s/ingress.yaml`

## Monitoring and Health Checks

### Health Endpoints
- **Liveness**: `https://your-hostname.com/api/v1/health/live`
- **Readiness**: `https://your-hostname.com/api/v1/health/ready`
- **Detailed**: `https://your-hostname.com/api/v1/health/detailed`

### Application URLs
- **Frontend**: `https://your-hostname.com`
- **API**: `https://your-hostname.com/api/v1`
- **API Docs**: `https://your-hostname.com/api/v1/swagger-ui.html`
- **Metrics**: `https://your-hostname.com/api/v1/actuator/prometheus`

### Monitoring Commands
```bash
# Check pod status
kubectl get pods -n kafka-tool

# View logs
kubectl logs -f deployment/kafka-web-app-backend -n kafka-tool

# Check ingress
kubectl get ingress -n kafka-tool

# Port forward for local testing
kubectl port-forward svc/kafka-web-app-backend 8080:8080 -n kafka-tool
```

## Scaling

### Horizontal Scaling
```bash
# Scale backend
kubectl scale deployment kafka-web-app-backend --replicas=3 -n kafka-tool

# Scale frontend
kubectl scale deployment kafka-web-app-frontend --replicas=3 -n kafka-tool
```

### Resource Limits
Adjust resource limits in `k8s/deployment.yaml`:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

## Troubleshooting

### Common Issues

**1. Database Connection Issues**
```bash
# Check PostgreSQL pod
kubectl logs -f deployment/postgres -n kafka-tool

# Test database connectivity
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- curl -f http://localhost:8080/api/v1/health/ready
```

**2. Kafka Connection Issues**
- Verify `KAFKA_BOOTSTRAP_SERVERS` configuration
- Check network connectivity from pods to Kafka cluster
- Ensure security configurations match your Kafka setup

**3. SSL Certificate Issues**
```bash
# Check certificate status
kubectl get certificate -n kafka-tool

# Check cert-manager logs
kubectl logs -f deployment/cert-manager -n cert-manager
```

**4. Image Pull Issues**
```bash
# Check if image exists
docker images | grep kafka-web-app-v2

# Update image pull policy
kubectl patch deployment kafka-web-app-backend -n kafka-tool -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend","imagePullPolicy":"Always"}]}}}}'
```

### Cleanup
```bash
# Remove entire deployment
./deploy.sh clean

# Or manually
kubectl delete namespace kafka-tool
```

## Security Considerations

1. **Secrets Management**: Use Kubernetes secrets or external secret management
2. **Network Policies**: Implement network policies to restrict pod communication
3. **RBAC**: Configure appropriate role-based access control
4. **Image Security**: Scan container images for vulnerabilities
5. **TLS**: Ensure all communication uses TLS encryption

## Performance Tuning

### JVM Settings
Adjust JVM settings in the Dockerfile:
```dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
```

### Database Tuning
- Adjust PostgreSQL configuration for your workload
- Monitor connection pool usage
- Consider read replicas for high-read workloads

### Kafka Optimization
- Configure appropriate consumer and producer settings
- Monitor consumer lag and throughput
- Adjust batch sizes and timeouts based on your use case
