# Deployment Guide

This guide provides step-by-step instructions for deploying the Kafka Web Tool v2.0 in various environments.

## 🚀 Quick Deployment Options

### Option 1: Docker Compose (Development/Testing)
```bash
# Clone and start
git clone <repository-url>
cd kafka-web-tool-v2
docker-compose up -d

# Access application
open http://localhost:3000
```

### Option 2: Kubernetes (Production)
```bash
# Deploy to Kubernetes
kubectl apply -f k8s/

# Check status
kubectl get pods -n kafka-tool
```

### Option 3: Helm Chart (Recommended)
```bash
# Add Helm repository
helm repo add kafka-web-tool https://charts.example.com/kafka-web-tool

# Install with custom values
helm install kafka-web-tool kafka-web-tool/kafka-web-tool -f values.yaml
```

## 🐳 Docker Compose Deployment

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ available RAM

### Step 1: Prepare Environment
```bash
# Create project directory
mkdir kafka-web-tool-v2
cd kafka-web-tool-v2

# Download deployment files
curl -O https://raw.githubusercontent.com/example/kafka-web-tool/main/docker-compose.yml
curl -O https://raw.githubusercontent.com/example/kafka-web-tool/main/.env.example
```

### Step 2: Configure Environment
```bash
# Copy and edit environment file
cp .env.example .env
nano .env
```

**Required Environment Variables:**
```bash
# Database Configuration
DB_HOST=postgres
DB_PORT=5432
DB_NAME=kafka_web_tool
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password

# Application Configuration
BACKEND_PORT=8080
FRONTEND_PORT=3000
LOG_LEVEL=INFO

# Kafka Configuration (Optional - can be configured via UI)
DEFAULT_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Step 3: Start Services
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f
```

### Step 4: Verify Deployment
```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl http://localhost:3000

# Access application
open http://localhost:3000
```

## ☸️ Kubernetes Deployment

### Prerequisites
- Kubernetes 1.24+
- kubectl configured
- 8GB+ cluster capacity

### Step 1: Create Namespace
```bash
# Create dedicated namespace
kubectl create namespace kafka-tool

# Set as default namespace (optional)
kubectl config set-context --current --namespace=kafka-tool
```

### Step 2: Configure Secrets
```bash
# Create database secret
kubectl create secret generic postgres-secret \
  --from-literal=username=postgres \
  --from-literal=password=your_secure_password \
  -n kafka-tool

# Create TLS secret (production)
kubectl create secret tls kafka-web-tool-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n kafka-tool
```

### Step 3: Deploy Database
```yaml
# postgres.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: kafka-tool
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_DB
          value: kafka_web_tool
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: kafka-tool
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: kafka-tool
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

### Step 4: Deploy Backend
```yaml
# backend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-web-app-backend
  namespace: kafka-tool
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kafka-web-app-backend
  template:
    metadata:
      labels:
        app: kafka-web-app-backend
    spec:
      containers:
      - name: backend
        image: your-registry/kafka-web-app-backend:latest
        env:
        - name: DB_HOST
          value: postgres
        - name: DB_PORT
          value: "5432"
        - name: DB_NAME
          value: kafka_web_tool
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-web-app-backend
  namespace: kafka-tool
spec:
  selector:
    app: kafka-web-app-backend
  ports:
  - port: 8080
    targetPort: 8080
```

### Step 5: Deploy Frontend
```yaml
# frontend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-web-app-frontend
  namespace: kafka-tool
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kafka-web-app-frontend
  template:
    metadata:
      labels:
        app: kafka-web-app-frontend
    spec:
      containers:
      - name: frontend
        image: your-registry/kafka-web-app-frontend:latest
        ports:
        - containerPort: 80
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 10
          periodSeconds: 10
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-web-app-frontend
  namespace: kafka-tool
spec:
  selector:
    app: kafka-web-app-frontend
  ports:
  - port: 80
    targetPort: 80
```

### Step 6: Configure Ingress
```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kafka-web-tool-ingress
  namespace: kafka-tool
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
spec:
  tls:
  - hosts:
    - kafkawebtool.yourdomain.com
    secretName: kafka-web-tool-tls
  rules:
  - host: kafkawebtool.yourdomain.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: kafka-web-app-backend
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: kafka-web-app-frontend
            port:
              number: 80
```

### Step 7: Deploy All Components
```bash
# Deploy in order
kubectl apply -f postgres.yaml
kubectl apply -f backend.yaml
kubectl apply -f frontend.yaml
kubectl apply -f ingress.yaml

# Check deployment status
kubectl get pods -n kafka-tool
kubectl get services -n kafka-tool
kubectl get ingress -n kafka-tool
```

## 🎛️ Helm Deployment (Recommended)

### Step 1: Add Helm Repository
```bash
# Add repository
helm repo add kafka-web-tool https://charts.example.com/kafka-web-tool
helm repo update
```

### Step 2: Create Values File
```yaml
# values.yaml
global:
  domain: kafkawebtool.yourdomain.com
  
database:
  enabled: true
  type: postgresql
  host: ""
  port: 5432
  name: kafka_web_tool
  username: postgres
  password: your_secure_password
  persistence:
    enabled: true
    size: 20Gi

backend:
  replicaCount: 3
  image:
    repository: your-registry/kafka-web-app-backend
    tag: latest
  resources:
    requests:
      memory: 512Mi
      cpu: 250m
    limits:
      memory: 1Gi
      cpu: 500m

frontend:
  replicaCount: 3
  image:
    repository: your-registry/kafka-web-app-frontend
    tag: latest
  resources:
    requests:
      memory: 128Mi
      cpu: 100m
    limits:
      memory: 256Mi
      cpu: 200m

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  tls:
    enabled: true
    secretName: kafka-web-tool-tls

monitoring:
  enabled: true
  prometheus:
    enabled: true
  grafana:
    enabled: true
```

### Step 3: Install with Helm
```bash
# Install
helm install kafka-web-tool kafka-web-tool/kafka-web-tool \
  -f values.yaml \
  --namespace kafka-tool \
  --create-namespace

# Check status
helm status kafka-web-tool -n kafka-tool

# Upgrade
helm upgrade kafka-web-tool kafka-web-tool/kafka-web-tool \
  -f values.yaml \
  -n kafka-tool
```

## 🔍 Post-Deployment Verification

### Health Checks
```bash
# Backend health
curl https://kafkawebtool.yourdomain.com/api/v1/actuator/health

# Frontend accessibility
curl https://kafkawebtool.yourdomain.com/

# Database connectivity
kubectl exec -it postgres-pod -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "SELECT 1;"
```

### Functional Tests
```bash
# Test API endpoints
curl https://kafkawebtool.yourdomain.com/api/v1/dashboard/metrics
curl https://kafkawebtool.yourdomain.com/api/v1/dashboard/health

# Test authentication (if enabled)
curl -X POST https://kafkawebtool.yourdomain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

## 🔧 Troubleshooting Deployment

### Common Issues

**Pod Not Starting:**
```bash
# Check pod status
kubectl describe pod <pod-name> -n kafka-tool

# Check logs
kubectl logs <pod-name> -n kafka-tool

# Check events
kubectl get events -n kafka-tool --sort-by='.lastTimestamp'
```

**Database Connection Issues:**
```bash
# Test database connectivity
kubectl exec -it <backend-pod> -n kafka-tool -- nc -zv postgres 5432

# Check database logs
kubectl logs postgres-pod -n kafka-tool
```

**Ingress Issues:**
```bash
# Check ingress status
kubectl describe ingress kafka-web-tool-ingress -n kafka-tool

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

## 📈 Scaling

### Horizontal Scaling
```bash
# Scale backend
kubectl scale deployment kafka-web-app-backend --replicas=5 -n kafka-tool

# Scale frontend
kubectl scale deployment kafka-web-app-frontend --replicas=5 -n kafka-tool
```

### Vertical Scaling
```bash
# Update resource limits
kubectl patch deployment kafka-web-app-backend -n kafka-tool -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend","resources":{"limits":{"memory":"2Gi","cpu":"1000m"}}}]}}}}'
```

---

**Next Steps:**
- [Configuration Guide](CONFIGURATION.md) - Customize your deployment
- [Security Guide](SECURITY.md) - Secure your installation
- [Monitoring Guide](MONITORING.md) - Set up monitoring and alerting
