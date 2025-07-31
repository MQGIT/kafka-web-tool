# 🚀 Quick Deploy Guide

## Overview

The **Quick Deploy** script (`quick-deploy.sh`) is designed for fast deployment using pre-built Docker images. This is perfect for:

- **Production deployments** with stable, tested images
- **Demo environments** for showcasing the application
- **Testing environments** where you don't need to build from source
- **Quick setup** when you want to get running fast

## 📋 Prerequisites

Before using the quick deploy script, ensure you have:

### Required Tools
- **kubectl** - Kubernetes command-line tool
- **Docker** - For image verification (optional)
- **Kubernetes cluster** - With proper access configured

### Cluster Requirements
- **Kubernetes 1.19+** recommended
- **Ingress controller** installed (nginx, traefik, etc.)
- **Storage class** available for PostgreSQL persistence
- **DNS configuration** for your hostname

## 🎯 Use Cases

### 1. **Production Deployment**
Deploy stable, tested images to production:
```bash
export REGISTRY="rmqk8"
export HOSTNAME="kafka-tool.yourcompany.com"
export NAMESPACE="kafka-production"
./quick-deploy.sh
```

### 2. **Demo Environment**
Quick setup for demonstrations:
```bash
export HOSTNAME="kafka-demo.yourcompany.com"
export NAMESPACE="kafka-demo"
./quick-deploy.sh
```

### 3. **Testing Environment**
Deploy for testing without building:
```bash
export HOSTNAME="kafka-test.yourcompany.com"
export NAMESPACE="kafka-test"
./quick-deploy.sh
```

## 🚀 Getting Started

### Step 1: Download the Script
```bash
# Clone the repository
git clone https://github.com/your-username/kafka-web-tool.git
cd kafka-web-tool

# Make the script executable
chmod +x quick-deploy.sh
```

### Step 2: Basic Deployment
```bash
# Interactive deployment (recommended for first-time users)
./quick-deploy.sh

# The script will prompt you for:
# - Docker registry (required - your DockerHub username or registry URL)
# - Application hostname
# - Kubernetes namespace (default: kafka-tool)
```

### Step 3: Environment Variable Deployment
```bash
# Set configuration via environment variables
export REGISTRY="your-dockerhub-username"  # Your Docker registry
export HOSTNAME="kafka-tool.example.com"   # Your domain
export NAMESPACE="kafka-tool"               # Kubernetes namespace

# Deploy without prompts
./quick-deploy.sh
```

## ⚙️ Configuration Options

### Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `REGISTRY` | Docker registry | `your-registry` | `your-dockerhub-username` |
| `HOSTNAME` | Application hostname | `your-hostname.com` | `kafka-tool.example.com` |
| `NAMESPACE` | Kubernetes namespace | `kafka-tool` | `kafka-production` |
| `BACKEND_IMAGE` | Backend image name | `kafka-web-app-v2` | `my-kafka-backend` |
| `FRONTEND_IMAGE` | Frontend image name | `kafka-web-app-frontend` | `my-kafka-frontend` |

### Command Options

```bash
# Deploy (default)
./quick-deploy.sh deploy

# Check deployment status
./quick-deploy.sh status

# Clean up deployment
./quick-deploy.sh clean
```

## 📊 What Gets Deployed

### Infrastructure Components
- **PostgreSQL** - Database for connection storage
- **Redis** - Caching and session storage
- **ConfigMaps** - Application configuration
- **Secrets** - Sensitive configuration data

### Application Components
- **Backend** - Java Spring Boot API (3 replicas)
- **Frontend** - React web application (3 replicas)
- **Services** - Load balancing and service discovery
- **Ingress** - External access with TLS termination

### Features Available
- ✅ **Message Browsing** - Real-time message viewing
- ✅ **Message Production** - Send messages to topics
- ✅ **Message Editing** - Modify existing messages
- ✅ **Message Deletion** - Send tombstone messages
- ✅ **Topic Management** - Create and manage topics
- ✅ **Connection Management** - Multiple Kafka clusters

### 🔐 **Default Login Credentials**
When you first access the application, you'll be presented with a login page. Use these default credentials:

- **Username**: `admin`
- **Password**: `admin123`

> ⚠️ **Security Note**: Change these default credentials in production environments by updating the application configuration.

## 🔧 Advanced Configuration

### Custom Registry
```bash
# Using Google Container Registry
export REGISTRY="gcr.io/your-project"
./quick-deploy.sh

# Using AWS ECR
export REGISTRY="123456789012.dkr.ecr.us-west-2.amazonaws.com"
./quick-deploy.sh

# Using Azure Container Registry
export REGISTRY="yourregistry.azurecr.io"
./quick-deploy.sh
```

### Multiple Environments
```bash
# Development environment
export NAMESPACE="kafka-dev"
export HOSTNAME="kafka-dev.example.com"
./quick-deploy.sh

# Staging environment
export NAMESPACE="kafka-staging"
export HOSTNAME="kafka-staging.example.com"
./quick-deploy.sh

# Production environment
export NAMESPACE="kafka-production"
export HOSTNAME="kafka.example.com"
./quick-deploy.sh
```

### Resource Customization
Edit the Kubernetes manifests in the `k8s/` directory before deployment:

```bash
# Customize resource limits
vim k8s/deployment.yaml

# Customize storage requirements
vim k8s/postgres.yaml

# Customize ingress configuration
vim k8s/ingress.yaml
```

## 🔍 Monitoring and Troubleshooting

### Check Deployment Status
```bash
# Overall status
./quick-deploy.sh status

# Detailed pod information
kubectl get pods -n your-namespace -o wide

# Check logs
kubectl logs -f deployment/kafka-web-app-backend -n your-namespace
kubectl logs -f deployment/kafka-web-app-frontend -n your-namespace
```

### Common Issues

#### 1. **Image Pull Errors**
```bash
# Check if images exist
docker pull rmqk8/kafka-web-app-v2:latest
docker pull rmqk8/kafka-web-app-frontend:latest

# Check registry authentication
docker login
```

#### 2. **DNS Resolution**
```bash
# Check ingress
kubectl get ingress -n your-namespace

# Check DNS configuration
nslookup your-hostname.com
```

#### 3. **Database Connection Issues**
```bash
# Check PostgreSQL status
kubectl get pods -n your-namespace -l app=postgres

# Check database logs
kubectl logs -f deployment/postgres -n your-namespace
```

## 🔒 Security Considerations

### Network Security
- Configure **network policies** to restrict pod-to-pod communication
- Use **TLS certificates** for ingress (Let's Encrypt recommended)
- Implement **authentication** for production environments

### Data Security
- Change **default passwords** in production
- Use **Kubernetes secrets** for sensitive data
- Enable **database encryption** at rest

### Access Control
- Configure **RBAC** for Kubernetes access
- Use **service accounts** with minimal permissions
- Implement **audit logging** for compliance

## 📈 Scaling

### Horizontal Scaling
```bash
# Scale backend
kubectl scale deployment kafka-web-app-backend --replicas=5 -n your-namespace

# Scale frontend
kubectl scale deployment kafka-web-app-frontend --replicas=5 -n your-namespace
```

### Vertical Scaling
Edit resource requests and limits in `k8s/deployment.yaml`:
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

## 🔄 Updates

### Rolling Updates
```bash
# Update to new image version
kubectl set image deployment/kafka-web-app-backend \
  kafka-web-app-backend=rmqk8/kafka-web-app-v2:v2.1.0 \
  -n your-namespace

# Check rollout status
kubectl rollout status deployment/kafka-web-app-backend -n your-namespace
```

### Rollback
```bash
# Rollback to previous version
kubectl rollout undo deployment/kafka-web-app-backend -n your-namespace
```

## 🆘 Support

### Getting Help
- **Documentation**: Check the main README.md
- **Issues**: Report bugs on GitHub Issues
- **Discussions**: Join GitHub Discussions for questions

### Useful Commands
```bash
# Get all resources
kubectl get all -n your-namespace

# Describe problematic pods
kubectl describe pod <pod-name> -n your-namespace

# Port forward for local testing
kubectl port-forward service/kafka-web-app-frontend 3000:80 -n your-namespace
```

## 🎯 **First Time Access**

After deployment completes successfully:

1. **Open your browser** and navigate to your application URL
2. **Login with default credentials**:
   - Username: `admin`
   - Password: `admin123`
3. **Start using the application**:
   - Add your first Kafka connection
   - Browse topics and messages
   - Try the edit/delete message features

> 💡 **Tip**: Bookmark your application URL for easy access!

---

**🎉 You're ready to deploy!** The quick deploy script will have you up and running in minutes with a fully functional Kafka Web Tool.
